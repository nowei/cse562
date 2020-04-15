import matplotlib.pyplot as plt 
import os
import glob
files = glob.glob('./graphs/*')
for f in files:
    os.remove(f)
arrays = []
with open('wave_data.txt', 'r') as f:
    for line in f:
        arr = eval(line)
        arrays.append(arr)
offset = 25
freq = 500
N = 4096
SR = 44100
start = int((freq / (SR / 2)) * N) - offset

ind_range = range(0, start + offset * 2)

multipliers = (SR / 2) / N
freq_buckets = [i * multipliers for i in ind_range]
x = freq_buckets
plt.rcParams["mathtext.fontset"] = "cm"
for i in range(len(arrays)):
    y = arrays[i]
    plt.plot(x, y)
    plt.xlabel('frequency (Hz)')
    plt.ylabel('amplitude')
    plt.grid()
    plt.title('i = {}'.format(i))
    plt.savefig('./graphs/{}.png'.format(i))
    plt.close()
