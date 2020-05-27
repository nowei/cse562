import sys
import time
from pyqtgraph.Qt import QtCore, QtGui
import numpy as np
import pyqtgraph as pg

import matplotlib.pyplot as plt

import time

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


class App(QtGui.QMainWindow):
    def __init__(self, parent=None):
        super(App, self).__init__(parent)

        #### Create Gui Elements ###########
        self.mainbox = QtGui.QWidget()
        self.setCentralWidget(self.mainbox)
        self.mainbox.setLayout(QtGui.QVBoxLayout())

        self.canvas = pg.GraphicsLayoutWidget()
        self.mainbox.layout().addWidget(self.canvas)

        self.label = QtGui.QLabel()
        self.mainbox.layout().addWidget(self.label)

        self.view = self.canvas.addViewBox()
        self.view.setAspectLocked(True)
        self.view.setRange(QtCore.QRectF(0,0, 100, 100))

        #  image plot
        self.img = pg.ImageItem(border='w')
        self.view.addItem(self.img)

        # self.canvas.nextRow()
        # #  line plot
        # self.otherplot = self.canvas.addPlot()
        # self.h2 = self.otherplot.plot(pen='y')


        #### Set Data  #####################

        self.x = np.linspace(0,50., num=100)
        self.X,self.Y = np.meshgrid(self.x,self.x)

        self.counter = 0
        self.fps = 0.
        self.lastupdate = time.time()

        self.prev = False

        #### Start  #####################
        self._update()

    def _update(self):

        self.data = np.sin(self.X/3.+self.counter/9.)*np.cos(self.Y/3.+self.counter/9.)
        self.ydata = np.sin(self.x/3.+ self.counter/9.)

        self.prev = not self.prev
        if self.prev:
            self.img.setImage(one)
            time.sleep(1)
        else:
            self.img.setImage(zero)
            time.sleep(0.001)

        # self.img.setImage(self.data)
        # self.h2.setData(self.ydata)

        now = time.time()
        dt = (now-self.lastupdate)
        if dt <= 0:
            dt = 0.000000000001
        fps2 = 1.0 / dt
        self.lastupdate = now
        self.fps = self.fps * 0.9 + fps2 * 0.1
        tx = 'Mean Frame Rate:  {fps:.3f} FPS'.format(fps=self.fps )
        self.label.setText(tx)
        QtCore.QTimer.singleShot(1, self._update)
        self.counter += 1


if __name__ == '__main__':

    app = QtGui.QApplication(sys.argv)
    thisapp = App()
    thisapp.show()
    sys.exit(app.exec_())
