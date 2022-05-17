# Assembler for Hack Computer
# Translates Hack Assembly code into binary

file = []

# Load file
filename = input("Enter filename: ")
with open(filename, 'r') as code:
	for line in code:
		file.append(line)

dest_dict = {'null': '000', 'M': '001', 'D': '010', 'MD': '011',
			'A': '100', 'AM': '101', 'AD': '110', 'AMD': '111'}

jump_dict = {'null': '000', 'JGT': '001', 'JEQ': '010', 'JGE': '011',
			 'JLT': '100', 'JNE': '101', 'JLE': '110', 'JMP': '111'}

comp_dict = {'0': '0101010', '1': '0111111', '-1': '0111010', 'D': '0001100', 'A': '0110000', 
			 '!D': '0001101', '!A': '0110001', '-D': '0001111', '-A': '0110011', 'D+1': '0011111', 
			 'A+1': '0110111', 'D-1': '0001110', 'A-1': '0110010', 'D+A': '0000010', 'D-A': '0010011', 
			 'A-D': '0000111', 'D&A': '0000000', 'D|A': '0010101', 
			 'M': '1110000', '!M': '1110001', '-M': '1110011', 'M+1': '1110111', 'M-1': '1110010', 
			 'D+M': '1000010', 'D-M': '1010011', 'M-D': '1000111', 'D&M': '1000000', 'D|M': '1010101'}

# Table with all variables and labels. Starts with built-ins
sym_table = {'R0': '@0', 'R1': '@1', 'R2': '@2', 'R3': '@3', 'R4': '@4', 'R5': '@5', 'R6': '@6',
			'R7': '@7', 'R8': '@8', 'R9': '@9', 'R10': '@10', 'R11': '@11', 'R12': '@12',
			'R13': '@13', 'R14': '@14', 'R15': '@15', 'SCREEN': '@16384', 'KBD': '@24576',
			'SP': '@0', 'LCL': '@1', 'ARG': '@2', 'THIS': '@3', 'THAT': '@4'}

def main():
	parsed_file = parser(file)
	
	file_without_labels = label_adder(parsed_file)
	file_with_variables = variable_adder(file_without_labels)

	final_file = ''
	for line in (file_with_variables):
		if line[0] == '@':
			final_file = final_file + a_instruction_handler(line) + '\n'
		else:
			final_file = final_file + c_instruction_handler(line) + '\n'

	final_file = final_file[:final_file.rfind('\n')] # gets rid of last empty line
	return final_file

def parser(file):
	'''
	Parses text and removes whitespace and comments
	'''
	file = [line[:line.find('//')] for line in file] # removes comments
	final = [line.strip() for line in file if line != ''] # removes empty lines and trailing spaces

	return final

def a_instruction_handler(instruction):
	'''
	Translates A-instruction to binary
	'''
	instruction = int(instruction.replace('@', ''))
	binary = ['0' for _ in range(16)]
	
	i = 1
	val = 16384 # 2^14 (highest value of last bit in binary)
	while instruction != 0:
		if val <= instruction:
			binary[i] = '1'
			instruction -= val
		val /= 2
		i+=1

	return ''.join(binary)


def c_instruction_handler(instruction):
	'''
	Translates C-instruction to binary
	'''
	binary = ['0' for _ in range(16)]
	for i in range(3):
		binary[i] = '1' # op-code

	dest_count = instruction.count('=')
	jump_count = instruction.count(';')
	
	if not(dest_count):
		instruction = 'null=' + instruction

	if not(jump_count):
		instruction = instruction + ';null'

	instruction = instruction.split('=') # splits into comp and rest
	instruction[1] = instruction[1].split(';') # splits 2nd into dest and jump
	dest_bits = dest_dict[instruction[0]]
	comp_bits = comp_dict[instruction[1][0]]
	jump_bits = jump_dict[instruction[1][1]]

	all_bits = [*comp_bits, *dest_bits, *jump_bits] # unpacks all and puts in 1 list
	binary[3:16] = all_bits

	return ''.join(binary)

def label_adder(file):
	'''
	Adds all labels to sym_table
	Labels are value like (xxx)
	Label's value is the line number of the next instruction
	Line numbers don't count whitespace or label declarations
	'''
	labels_to_remove = [] # keeps track of labels to remove
	line_number = 0
	for line in file:
		if line.startswith('('):
			# is a label
			label_name = line.strip('()')
			sym_table[label_name] = line_number
			labels_to_remove.append(line)
		else:
			line_number += 1 # not a label

	file = [line for line in file if line not in labels_to_remove] # removes all label declarations
	return file

def variable_adder(file):
	'''
	Adds variables to sym_table
	Also replaces all variables/labels with @ and their value
	'''
	address = 16
	for i in range(len(file)):
		line = file[i]
		if line.startswith('@'):
			variable_name = line.strip('@')
			try:
				# make sure variable is not a number
				int(variable_name)
			except:
				if variable_name not in sym_table:
					# variable is not already added or is a label
					sym_table[variable_name] = address
					address += 1
				# Replaces variable/label name with value
				file[i] = '@' + str(sym_table[variable_name])

	return file


# Write to file
file_to_write = filename[filename.find('/')+1:filename.find('.')] + '.hack' 
with open(file_to_write, 'w') as f:
	f.write(main())