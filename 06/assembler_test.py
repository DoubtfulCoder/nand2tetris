import unittest
from assembler import *

class TestAssembler(unittest.TestCase):
	# def test_parser(self):
	# 	pass
	def test_a_instruction(self):
		self.assertEqual(a_instruction_handler('@21'), '0000000000010101')
		self.assertEqual(a_instruction_handler('@16383'), '0011111111111111')
		self.assertEqual(a_instruction_handler('@1'), '0000000000000001')
	def test_c_instruction(self):
		self.assertEqual(c_instruction_handler('MD=D+1'), '1110011111011000')
		self.assertEqual(c_instruction_handler('M+1;JEQ'), '1111110111000010')
		self.assertEqual(c_instruction_handler('!A;JLT'), '1110110001000100')
		self.assertEqual(c_instruction_handler('AMD=!A;JMP'), '1110110001111111')
		self.assertEqual(c_instruction_handler('D=M'), '1111110000010000')

if __name__ == '__main__':
    unittest.main()