# Performs VM translations for branching and functions operations

import BasicTranslator

i = 0 # running integer for number of calls - used for return address

is_function = [False, None] # Used for labels in functions; 1st val: func or not, 2nd val: name of func

def label(line):
	'''
	Creates a label
	- If label is inside a function, label is formatted as functionName$label
	'''
	label = line.replace('label ', '', 1)
	if is_function[0]:
		label = is_function[1] + '$' + label
	return f'({label})'

def goto(line):
	'''
	Does an unconditional goto label
	- If goto is inside a function, label is formatted as functionName$label
	'''
	label = line.replace('goto ', '', 1)
	if is_function[0]:
		label = is_function[1] + '$' + label
	return f"""
	@{label}
	0;JMP"""
	
def if_goto(line):
	'''
	Does a jump to label only if top value on stack is not 0 (meaning true)
	- If if-goto is inside a function, label is formatted as functionName$label
	'''
	label = line.replace('if-goto ', '', 1)
	if is_function[0]:
		label = is_function[1] + '$' + label
	return f"""
	@SP
	M=M-1
	A=M
	D=M
	@{label}
	D;JNE
	"""

def function_case(line):
	'''
	1. Creates a label for function name
	2. Sets local to SP
	3. Stores num_vars in temp storage and then loops num_vars 0s onto local storage 
	'''
	function_parts = line.split()
	function_name = function_parts[1]
	num_vars = function_parts[2]
	is_function[:] = [True, function_name]
	return f"""
	({function_name})
	@SP
	D=M
	@LCL
	M=D
	@{num_vars}
	D=A
	@10
	M=D
	({function_name}$PUSH_NVARS)
	@10
	D=M
	@{function_name}$END
	D;JEQ
	@SP
	A=M
	M=0
	@10
	M=M-1
	@SP
	M=M+1
	@{function_name}$PUSH_NVARS
	0;JMP
	({function_name}$END)
	"""

def call_case(line):
	'''
	Saves frame (return address, LCL, ARG, THIS, THAT)
	Pops top num_vars values from stack onto argument storage
	'''
	function_parts = line.split()
	function_name = function_parts[1]
	num_vars = function_parts[2]
	global i
	i+=1
	return f"""
	@{function_name}$ret.{i}
	D=A
	@SP
	A=M
	M=D
	@SP
	M=M+1
	@LCL
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	@ARG
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	@THIS
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	@THAT
	D=M
	@SP
	A=M
	M=D
	@SP
	M=M+1
	D=M
	@LCL
	M=D
	@SP
	D=M
	@5
	D=D-A
	@{num_vars}
	D=D-A
	@ARG
	M=D
	@{function_name}
	0;JMP
	({function_name}$ret.{i})
	"""

def return_case():
	'''
	1. Saves return address in temp storage in case pop_arg overrides it
	2. Pops onto argument 0 because return value replaces args in net exchange
	3. Sets SP to ARG value + 1 (right after return value)
	4. Goes to @LCL for the endframe value in order to restore saved frame
	5. Jumps back to return address in code
	'''
	# is_function[0] = False
	save_ret_addr = """
	@5
	D=A
	@LCL
	A=M-D
	D=M
	@11
	M=D"""
	pop_arg = BasicTranslator.operation("pop argument 0", "")
	# Setting SP back
	sp_set = """
	@ARG
	D=M+1
	@SP
	M=D"""
	# Restoring frame
	restore_frame = """
	@LCL
	A=M-1
	D=M
	@THAT
	M=D
	@2
	D=A
	@LCL
	A=M-D
	D=M
	@THIS
	M=D
	@3
	D=A
	@LCL
	A=M-D
	D=M
	@ARG
	M=D
	@4
	D=A
	@LCL
	A=M-D
	D=M
	@LCL
	M=D"""
	# Jump back to return address in code
	jump_to_ret = f"""
	@11
	A=M
	0;JMP
	"""
	return save_ret_addr + pop_arg + sp_set + restore_frame + jump_to_ret