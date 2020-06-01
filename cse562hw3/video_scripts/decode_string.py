import argparse

parser = argparse.ArgumentParser()
parser.add_argument('s', type=str)
# parser.print_help()
args = parser.parse_args()

binary = args.s

print('decoded string: {}'.format(''.join([chr(int(binary[i:i+8],2)) for i in range(0, len(binary), 8)])))