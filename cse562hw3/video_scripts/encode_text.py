import numpy as np
from matplotlib import pyplot as plt
from matplotlib import animation

nx = 150
ny = 50
alpha = 0.5


fig = plt.figure()
data = plt.imread('background.jpg').copy() / 255 
print(data.shape)
data = np.dstack((data, np.ones((data.shape[0], data.shape[1]))))
zero = data.copy()
for x in range(data.shape[0]):
    for y in range(data.shape[1]):
        data[x, y, 3] = 1 - alpha

one = data

im = plt.imshow(data, vmin=0, vmax=1)

def init():
    im.set_data(data)

status = 0

def animate(i):
    global status
    status = not status
    if status:
        im.set_data(one)
    else:
        im.set_data(zero)
    # print('nani')
    return im

anim = animation.FuncAnimation(fig, animate, init_func=init, frames=nx * ny,
                               interval=0)
plt.show()