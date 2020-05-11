import numpy as np
import matplotlib.pyplot as plt
import json

name = '2.txt'
f = open(name, 'r')
data = json.loads(f.read())
t = []
x = []
y = []
for point in data :
	t.append(point[2])
	x.append(point[0])
	y.append(point[1])

fig = plt.figure()
ax = fig.add_subplot(111)

ax.scatter(x, y, s=10, c='orange', marker='s', label='x')
# ax.scatter(t,y, s=10, c='r', marker='o', label='y')
ax.set_ylabel('y')
ax.set_xlabel('x')
plt.legend(loc='upper left')
plt.title('x,y coordiantes of ball1')
plt.show()