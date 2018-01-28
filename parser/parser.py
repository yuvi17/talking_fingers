#imports
import nltk
import numpy as np
import os
from flask import Flask
from flask import request
from flask import jsonify
from autocorrect import spell
from nltk.stem import PorterStemmer
from nltk.corpus import wordnet as wn
from nltk.parse.stanford import StanfordParser
from nltk.tree import ParentedTree, Tree
from nltk import pos_tag,word_tokenize
from nltk.stem import WordNetLemmatizer


# set env variables
os.environ['STANFORD_PARSER'] = '/usr/local/Cellar/stanford-parser/3.8.0/libexec/'
os.environ['STANFORD_MODELS'] = '/usr/local/Cellar/stanford-parser/3.8.0/libexec/'


#initializations
lemmatizer = WordNetLemmatizer()
parser = StanfordParser()
ps = PorterStemmer()






# parsing functions

def find_subject(t):
	for s in t.subtrees(lambda t: t.label() == 'NP'):
		for n in s.subtrees(lambda n: n.label().startswith('NN')):
			if(n != None):
				return (n[0], find_attrs(n))
		for n in s.subtrees(lambda n: n.label().startswith('PRP')):
			if(n != None):
				return (n[0], find_attrs(n))

def findQuestionMatter(t):
	ans = []
	for s in t.subtrees(lambda t: t.label().startswith("WH")):
		for n in s.subtrees(lambda n: n.label().startswith('WHADJP')):
			for n in s.subtrees((lambda n: n.label() == ('JJ'))):
				ans.append(n[0])
		for n in s.subtrees((lambda n: n.label().startswith('NN'))):
			if((n[0],find_attrs(n)) not in ans):
				ans.append(n[0])
		for n in s.subtrees(lambda n: n.label().startswith('WH')):
			for n in s.subtrees((lambda n: n.label() == ('WRB'))):
				ans.append(n)
			for n in s.subtrees((lambda n: n.label().startswith('WP'))):
				ans.append(n)
	return ans

# Depth First Search the tree and take the last verb in VP subtree.
def find_predicate(t):
	v = None

	for s in t.subtrees(lambda t: t.label() == 'VP'):
		for n in s.subtrees(lambda n: n.label().startswith('VB')):
			v = n
	if(v != None):
		return (v[0], find_attrs(v))
 
# Breadth First Search the siblings of VP subtree
# and take the first noun or adjective
def find_object(t):
	for s in t.subtrees(lambda t: t.label() == 'VP'):
		for n in s.subtrees(lambda n: n.label() in ['NP', 'PP', 'ADJP']):
			if n.label() in ['NP', 'PP']:
				for c in n.subtrees(lambda c: c.label().startswith('NN')):
					return (c[0], find_attrs(c))
			else:
				for c in n.subtrees(lambda c: c.label().startswith('JJ')):
					return (c[0], find_attrs(c))

def find_attrs(node):
	attrs = []
	p = node.parent()

# Search siblings of adjective for adverbs
	if node.label().startswith('JJ'):
		for s in p:
			if s.label() == 'RB':
				attrs.append(s[0])

	elif node.label().startswith('NN'):
		for s in p:
			if s.label() in ['DT','PRP$','POS','JJ','CD','ADJP','QP','NP']:
				attrs.append(s[0])

	elif node.label().startswith('NP'):
		for s in p:
			if s.label() in ['PRP$','NN']:
				attrs.append(s[0])

# Search siblings of verbs for adverb phrase
	elif node.label().startswith('VB'):
		for s in p:
			if s.label() == 'ADVP':
				attrs.append(' '.join(s.flatten()))

# Search uncles
# if the node is noun or adjective search for prepositional phrase
	if node.label().startswith('JJ') or node.label().startswith('NN'):
		for s in p.parent():
			if s != p and s.label() == 'PP':
				attrs.append(' '.join(s.flatten()))

	elif node.label().startswith('VB'):
		for s in p.parent():
			if s != p and s.label().startswith('VB'):
				attrs.append(' '.join(s.flatten()))

	return attrs
	


def parseSentence(sentence):
	import re
	stopwords = ["are","is","am","was","were","a","an","the","do","did","Did"]
	
	replace = {
		'many' : 'number',
		'I' : 'me',
		'my' : 'me'
	}

	negations = ['not','no',"n't","none"]
	
	stopwords = set(stopwords)
	res = list(parser.raw_parse(sentence))[0]
	print(res)
	res = ParentedTree.convert(res)
	res.pretty_print()
	subjects = find_subject(res)
	if(subjects == None):
		subjects = ("",[])
	objects = find_object(res)
	if(objects == None):
		objects = ("",[])
	verbs = find_predicate(res)
	if(verbs == None):
		verbs = ("",[])
	attributes = find_attrs(res)
	if(attributes == None):
		attributes = ("",[])
	ques = findQuestionMatter(res)
	if(ques == None):
		ques = ("",[])
	print(subjects)
	print(objects)
	print(verbs)
	print(attributes)
	print(ques)
	tags = pos_tag(word_tokenize(sentence))
	print(tags)
	
	sent = ""
	for s in subjects[1]:
		if(s not in stopwords):
			sent = sent + " " + s + " "
	if(subjects[0] not in sent):
		sent = sent + " " + subjects[0] + " "
	for o in objects[1]:
		if(o not in stopwords and o not in sent):
			sent = sent + " " + o + " "
	if(objects[0] not in sent and objects[0] not in stopwords):
		sent = sent + " " + (objects[0]) + " "
	
	for v in verbs[1]:
		if(v not in stopwords):
			sent = sent + " " + ps.stem(v) + " "
	if(ps.stem(verbs[0]) not in sent and ps.stem(verbs[0]) not in stopwords):
		sent = sent + " " + ps.stem(verbs[0]) + " "
	for q in ques:
		if(q[0] not in stopwords and q[0] not in set(sent)):
			sent = sent + " " + q[0] + " "
	resp = re.sub( '\s+', ' ', sent ).strip()
	for s in tags:
		print(s)
		if s[0] in negations:
			resp = resp + " " + "no"
	r2 = ''
	for r in resp.split(" "):
		print(spell(r))
		print(r)
		r2 = r2 + " " + spell(r)
	return r2

app = Flask(__name__)

@app.route('/parse')
def index():
	sentence = request.args.get('sentence')
	print(sentence)
	isle = parseSentence(sentence)
	return jsonify({'response' : isle})

if __name__ == '__main__':
	app.run(debug=True,host='0.0.0.0')
