# Performs VM translations for arithmetic and memory operations

from Branching_Functions import *

# Logical/arithmetic opertions
operations = {'add': '+', 'sub': '-', 'or': '|', 'and': '&',
			'eq': 'JEQ', 'lt': 'JLT', 'gt': 'JGT'}

# Segment names and corresponding symbols
segments = {'local': 'LCL', 'argument': 'ARG', 'this': 'THIS', 'that': 'THAT', 'temp': '5'}

n = 0 # starting value for differentiating lables for eq, lt, gt 

def parser(file):
	'''
	Parses text and removes whitespace and comments
	'''
	final = []
	file = [line[:line.find('//')] for line in file] # grabs code in each line before comment
	for line in file:
		if line != '':
			final.append(line.strip()) # if line is not empty, append it with trailing whitespace removed

	return final

def operation(line, file):
	'''
	Converts VM instruction to assembly instructions
	Implements arithmetic, logical, memory, branching, and function operations
	File argument used for filename for static segment
	'''
	if line.startswith('push'):
		line = line.split() # splits into parts: push, segment, i
		segment = line[1]
		i = line[2]
		if segment == 'constant':
			return push_contant(i)
		elif segment == 'temp':
			return push_temp(i)
		elif segment == 'pointer':
			return push_pointer(i)
		elif segment == 'static':
			file = file.replace('.asm', '')
			return push_static(i, file)
		else:
			return push_other(segment, i)
	elif line.startswith('pop'):
		line = line.split() # splits into parts: pop, segment, i
		segment = line[1]
		i = line[2]
		if segment == 'temp':
			return pop_temp(i)
		elif segment == 'pointer':
			return pop_pointer(i)
		elif segment == 'static':
			file = file.replace('.asm', '')
			return pop_static(i, file)
		else:
			return pop_other(segment, i)
	elif line.startswith('label'):
		return label(line)
	elif line.startswith('goto'):
		return goto(line)
	elif line.startswith('if-goto'):
		return if_goto(line)
	elif line.startswith('function'):
		return function_case(line)
	elif line.startswith('call'):
		return call_case(line)
	elif line.startswith('return'):
		return return_case()
	elif line == 'neg':
		return """
		@SP
		A=M-1
		M=-M
		"""
	elif line == 'not':
		return """
		@SP
		A=M-1
		M=!M
		"""
	elif line == 'eq' or line == 'lt' or line == 'gt':
		sign = operations[line]
		global n
		n += 1 # value for differentiating labels
		return f"""
		(CHECK{n})
			@SP
			A=M-1
			D=M
			A=A-1
			D=M-D
			M=0
			@SP
			M=M-1
			@EQUAL{n}
			D;{sign}
			@END{n}
			0;JMP
		(EQUAL{n})
			@SP
			A=M-1
			M=-1
		(END{n})
			@SP
			A=M
		"""
	else:
		# Implements operations for add, sub, and, or
		sign = operations[line]

		# Computes xSIGNy
		return f"""
		@SP
		A=M-1
		D=M
		A=A-1
		M=M{sign}D
		@SP
		M=M-1
		"""

def push_contant(i):
	return f"""
	@{i}
	D=A
	@SP
	A=M
	M=D
	@SP
	M=M+1
	"""

def push_temp(i):
	return f'''
	@5
	D=A
	@{i}
	A=A+D
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	'''

def pop_temp(i):
	return f"""
	@5
	D=A
	@{i}
	D=D+A
	@SP
	A=M
	M=D
	A=A-1
	D=M
	A=A+1
	A=M
	M=D
	@SP
	M=M-1
	"""

def push_pointer(to_push):
	this_or_that = 'THAT' if int(to_push) else 'THIS'
	return f"""
	@{this_or_that}
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	"""

def pop_pointer(to_pop):
	this_or_that = 'THAT' if int(to_pop) else 'THIS'
	return f"""
	@SP
	M=M-1
	A=M
	D=M
	@{this_or_that}
	M=D
	"""

def push_static(i, file):
	return f"""
	@{file}.{i}
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	"""

def pop_static(i, file):
	return f"""
	@SP
	M=M-1
	A=M
	D=M
	@{file}.{i}
	M=D
	"""

def push_other(segment, i):
	'''
	Implements push segment/local/this/that
	pushes ith value in segment to stack
	'''
	seg = segments[segment] 
	return f"""
	@{i}
	D=A
	@{seg}
	A=M+D
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	"""

def pop_other(segment, i):
	'''
	Implements pop segment/local/this/that
	'''
	seg = segments[segment] 
	return f"""
	@{i}
	D=A
	@{seg}
	D=D+M
	@SP
	A=M
	M=D
	A=A-1
	D=M
	A=A+1
	A=M
	M=D
	@SP
	M=M-1
	"""

'''
1 problem:
1. labels automatically assuming function
'''