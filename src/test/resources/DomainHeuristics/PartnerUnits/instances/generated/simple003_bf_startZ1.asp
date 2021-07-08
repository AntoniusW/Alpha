maxPU(2).

comUnit(1).
comUnit(2).
comUnit(3).

zone2sensor(1,1).
zone2sensor(1,2).
zone2sensor(2,1).
zone2sensor(2,3).
zone2sensor(2,4).
zone2sensor(3,3).


% breadth-first ordering:
maxLayer(4).
sensorLayer(1,1).
sensorLayer(2,1).
sensorLayer(3,3).
sensorLayer(4,3).
zoneLayer(1,0).
zoneLayer(2,2).
zoneLayer(3,4).
