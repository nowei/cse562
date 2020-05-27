#!/usr/local/bin/python3
import numpy as np
# import cv2
from matplotlib import pyplot as plt
import time
from random import randrange

alpha = 0.5
data = plt.imread('background.jpg').copy() / 255 
data = np.dstack((data, np.ones((data.shape[0], data.shape[1]))))
# data = np.ones((100, 100))

# time_scale = 0.01 # seconds

zero = data.copy()
for x in range(data.shape[0]):
    for y in range(data.shape[1]):
        data[x, y, 3] = 1 - alpha

one = data
# one = np.zeros(data.shape)

# print('one',one[:,:,3])
# print('zero',zero[:,:,3])

print(one)
print(zero)

prev = False

# def sin2d(x,y):
#     """2-d sine function to plot"""
#     return np.sin(x) + np.cos(y)

def getFrame(bit, obj):
    global prev
    # """Generate next frame of simulation as numpy array"""

    # # Create data on first call only
    # if getFrame.z is None:
    #     xx, yy = np.meshgrid(np.linspace(0,2*np.pi,800), np.linspace(0,2*np.pi,800))
    #     getFrame.z = sin2d(xx, yy)
    #     getFrame.z = cv2.normalize(getFrame.z,None,alpha=0,beta=1,norm_type=cv2.NORM_MINMAX, dtype=cv2.CV_32F)

    # # Just roll data for subsequent calls
    # getFrame.z = np.roll(getFrame.z,(1,2),(0,1))
    # return getFrame.z
    # if getFrame.z is None:
        # getFrame.z = np.ones((100, 100))
        # time.sleep(2)

    swap_time = 0.01
    if bit:
        swap_time = swap_time / 2
    t_end = time.time() + 1
    while time.time() < t_end:
        prev = not prev 
        if prev:
            # print('nani0')
            # cv2.imshow('image', one)
            obj.set_data(one)
        else:
            # print('nani1')
            # cv2.imshow('image', zero)
            obj.set_data(zero)
        # print('swapping to {} with {}'.format(prev, swap_time))
        plt.pause(0.001)
        print('a')
        time.sleep(swap_time)

# getFrame.z = None

def main(): 
    plt.ion()
    obj = plt.imshow(zero)
    plt.show()
    while True:
        bit = randrange(2)
        print(bit)
    #     # Get a numpy array to display from the simulation
        getFrame(bit, obj)
        # cv2.waitKey(1)
    # cv2.imshow('image', one)
    # while True:
    # #     # print('swapping')
    # #     # cv2.imshow('image', one)
    # #     # time.sleep(3)
    # #     # print('swapping')
    # #     # cv2.imshow('image', zero)
    # #     # time.sleep(3)
    #     print('swap')
    #     obj.set_data(one)
    #     plt.pause(0.01)
    #     time.sleep(2)
    #     print('swap')
    #     obj.set_data(zero)
    #     plt.pause(0.01)
    #     time.sleep(0.001)


if __name__ == '__main__':
    main()