import matplotlib.pyplot as plt

# Coordinates provided
coordinates = [
    (50000, 510000), (320000, 510000), (50000, 440000), (130000, 440000), (220000, 440000),
    (320000, 440000), (420000, 380000), (320000, 380000), (220000, 380000), (220000, 320000),
    (130000, 320000), (50000, 320000), (50000, 50000), (130000, 190000), (220000, 190000),
    (320000, 320000), (320000, 260000), (420000, 320000), (320000, 190000), (320000, 50000),
    (220000, 50000), (220000, 130000), (130000, 130000), (130000, 50000)
]

# Plotting the coordinates
plt.figure(figsize=(10, 10))
for idx, (x, y) in enumerate(coordinates):
    plt.scatter(x, y, label=f'Point {idx}')
plt.xlabel('X Coordinate')
plt.ylabel('Y Coordinate')
plt.title('Grid Representation of Coordinates')
plt.legend()
plt.grid(True)
plt.show()