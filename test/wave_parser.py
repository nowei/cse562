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
offset = 15
freq = 18000
N = 8192
SR = 44100
observer = int((freq / (SR / 2)) * (N // 2)) - offset
ind_range = range(observer, observer + offset * 2)

multiplier = (SR // 2) / (N // 2)
freq_buckets = [i * multiplier for i in ind_range]
print(multiplier, observer + offset, freq_buckets[offset])
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
