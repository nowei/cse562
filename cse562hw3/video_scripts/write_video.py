import numpy as np
import cv2
from matplotlib import pyplot as plt
import time

# This only works because of this guy: https://github.com/ContinuumIO/anaconda-issues/issues/223#issuecomment-285523938

img = cv2.imread('background.jpg')
on = img.astype('uint8')
alpha = 0.1
off = (on * (1 - alpha) + np.zeros(on.shape) * alpha).astype('uint8')

one = [off, on, off, on, off, on]

zero = [off, off, on, off, off, on]

def create_video(encode_string):

    output_path = './videos/'
    output_video_file = output_path + 'test.avi'
    height, width, layers = on.shape
    frame_rate = 60

    fourcc = cv2.VideoWriter_fourcc(*'MJPG')
    videowriter = cv2.VideoWriter(output_video_file, fourcc, frame_rate, (width, height))
    print('string to convert: {}'.format(encode_string))
    binary = ''.join(format(ord(x), 'b') for x in encode_string)
    print('binary representation: {}'.format(binary))
    for b in binary:
        if b == '0':
            for frame in zero:
                videowriter.write(frame)
        else:
            for frame in one:
                videowriter.write(frame)
    videowriter.release()

create_video('I wasted 3 hours getting video to work')