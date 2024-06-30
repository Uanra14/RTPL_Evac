# RTPL_Evac
Thesis project. Optimization of pick-up location decisions for public transit evacuation with buses. Coded on Java using Gurobi Solver.

The two jupyter notebooks were used for data scraping, processing and visualisation. The Robust Transit Pick-up Location (RTPL) and WCD class were used fo the replication of the results by Kulsrethra et al. (2014). The ZDRTPL and ZDWCD are the classes corresponding to the Smart Robust Transit Pick-up Location Problem (SRTPL), the extension introduced in this thesis. The main class contains the code necessary for running all 4 model instances: RTPL in Sioux Falls, RTPL in Rotterdam, SRTPL in Sioux Falls, and SRTPL in Rotterdam. 

The CSV file "Demands Rotterdam" contains the values used for the demands of the Rotterdam network. The file "Rotterdam Network All Coordinates" contains the coordinate of all the nodes in the Rotterdam network. There are 57 nodes in the network; the first 7 nodes are shelter locations, the other 50 are evenly split into types, the first 10 are type 1, the next 10 are type 2, and so on.
