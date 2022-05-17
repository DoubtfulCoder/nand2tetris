# VM Translator for Hack Computer
# Translates VM code to Hack Assembly code

import sys
import os
import inspect
from BasicTranslator import *

def extract_text(filename):
	# extracts text from VM file
	file = []
	with open(filename, 'r') as vm_code:
		for line in vm_code:
			file.append(line)
	return file

def translate(file, filename):
	# Adds operation code in assembly
	code = ''
	for line in file:
		assembly_code = operation(line, filename)
		assembly_code = inspect.cleandoc(assembly_code) + '\n' # removes trailing tabs
		code += assembly_code
	return code

def main():
	# Extract VM code from file or directory
	arg = sys.argv[1]
	if arg.endswith('vm'):
		vm_code = extract_text(arg)
		asm_file = arg[arg.rfind('\\')+1:arg.find('.')] + '.asm'
		final_file = ''
		parsed_file = parser(vm_code)
		final_file += translate(parsed_file, asm_file)
	else:
		bootstrap_code_1 = inspect.cleandoc("""
		@256
		D=A
		@SP
		M=D""") + '\n'
		bootstrap_code_2 = inspect.cleandoc(operation("call Sys.init 0", "")) + '\n'
		final_file = bootstrap_code_1 + bootstrap_code_2
		for file in os.listdir(arg):
			if file.endswith('vm'):
				path = os.path.join(arg, file)
				parsed_code = parser(extract_text(path))
				final_file += translate(parsed_code, file)
		asm_file = arg[arg.rfind('\\')+1:] + '.asm'
		asm_file = os.path.join(arg, asm_file) # putting file in directory

	# Write to file
	with open(asm_file, 'w') as f:
		f.write(final_file)



# Run main
main()