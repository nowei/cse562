import matplotlib.pyplot as plt

def plot_graph(title, data):
    print('plotting {}'.format(title))
    time = data['time']
    pitch = data['pitch']
    roll = data['roll']
    plt.plot(time, pitch, label='pitch')
    plt.plot(time, roll, label='roll')
    plt.grid()
    plt.legend()
    plt.xlabel('time (minutes)')
    plt.ylabel('degrees')
    plt.title('time vs. tilt (pitch and roll) using {}'.format(title))
    plt.savefig(title.replace(' ', '_') + ".png")
    plt.close()

def parse_csv(file_name):
    m = {"time": [],
         "pitch": [],
         "roll": []}
    print('parsing {}'.format(file_name))
    min_time = None
    with open(file_name, 'r') as f:
        for line in f:
            time, pitch, roll = line.split(',')
            time = time.replace('E', 'e')
            if min_time == None:
                min_time = float(time)
            m['time'].append((float(time) - min_time) / 1000 / 60)
            m['pitch'].append(float(pitch))
            m['roll'].append(float(roll))
    return m

accel = parse_csv('accelCDT.csv')
gyro = parse_csv('gyroCDT.csv')
comp = parse_csv('compCDT.csv')

plot_graph('accelerometer', accel)
plot_graph('gyroscope', gyro)
plot_graph('complementary filter', comp)