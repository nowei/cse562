import numpy as np
import cv2
from matplotlib import pyplot as plt
import time

# This only works because of this guy: https://github.com/ContinuumIO/anaconda-issues/issues/223#issuecomment-285523938

img = cv2.imread('background.jpg')
on = img.astype('uint8')
alpha = 0.1
black = np.zeros(on.shape, dtype='uint8')
off = (on * (1 - alpha) + black * alpha).astype('uint8')

# Original
one = [off, on, off, on, off, on]
zero = [off, off, on, off, off, on]

# Doubled up
# one = [off, off, on, on, off, off, on, on, off, off, on, on]
# zero = [off, off, off, off, on, on, off, off, off, off, on, on]

# Attempt 2

def create_video(encode_string):

    output_path = './videos/'
    output_video_file = output_path + 'plzwork_{}_30fps.avi'.format(alpha)
    print('writing video to {}'.format(output_video_file))
    height, width, layers = on.shape
    frame_rate = 30

    fourcc = cv2.VideoWriter_fourcc(*'MJPG')
    videowriter = cv2.VideoWriter(output_video_file, fourcc, frame_rate, (width, height))
    print('string to convert: {}'.format(encode_string))
    binary = ''.join(format(ord(i),'b').zfill(8) for i in encode_string)
    print('binary representation: {}'.format(binary))
    print('decoded string: {}'.format(''.join([chr(int(binary[i:i+8],2)) for i in range(0, len(binary), 8)])))
    videowriter.write(black)
    REPEAT = 3
    for b in binary:
        if b == '0':
            # for i in range(REPEAT):
            for frame in zero:
                videowriter.write(frame)
        else:
            # for i in range(REPEAT):
            for frame in one:
                videowriter.write(frame)
    for i in range(30):
        videowriter.write(black)
    videowriter.release()

create_video('plzwork')