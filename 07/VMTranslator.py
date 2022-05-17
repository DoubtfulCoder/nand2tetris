# VM Translator for Hack Computer
# Translates VM code to Hack Assembly code

import sys
import inspect

# Logical/arithmetic opertions
operations = {'add': '+', 'sub': '-', 'or': '|', 'and': '&',
			'eq': 'JEQ', 'lt': 'JLT', 'gt': 'JGT'}

# Segment names and corresponding symbols
segments = {'local': 'LCL', 'argument': 'ARG', 'this': 'THIS', 'that': 'THAT', 'temp': '5'}

n = 0 # starting value for differentiating lables for eq, lt, gt 

def main():
	file = []
	vm_filename = sys.argv[1]
	with open(vm_filename, 'r') as vm_code:
		for line in vm_code:
			file.append(line)

	parsed_file = parser(file)

	final_file = ''
	for line in parsed_file:
		# Adds operation code in assembly + a comment what the operation is in VM
		assembly_code = operation(line)
		assembly_code = inspect.cleandoc(assembly_code) + '\n' # removes trailing tabs
		final_file = final_file + assembly_code

	# Write to file
	asm_file = vm_filename[vm_filename.rfind('\\')+1:vm_filename.find('.')] + '.asm'
	print(asm_file)
	with open(asm_file, 'w') as f:
		f.write(final_file)

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

def operation(line):
	'''
	Converts VM instruction to assembly instructions
	Works for arithmetic operations, logical operations, and push operation
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
			return push_static(i)
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
			return pop_static(i)
		else:
			return pop_other(segment, i)

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

def push_static(i):
	return f"""
	@static.{i}
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	"""

def pop_static(i):
	return f"""
	@SP
	M=M-1
	A=M
	D=M
	@static.{i}
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

# Run main
main()