maxPU(2).

comUnit(1). comUnit(2).

% relation between zones and door sensors
zone2sensor(1,1).
zone2sensor(1,2).
zone2sensor(2,1).
zone2sensor(2,3).
zone2sensor(2,4).
zone2sensor(3,3).

% helpers for QUICKPUP heuristics, inspired by quickpup.ascass by Erich Teppan:
sensor(S):-zone2sensor(Z,S).
zone(Z):-zone2sensor(Z,S).

numZones(N) :- zone(N), not zone(N+1).
numSensors(N) :- sensor(N), not sensor(N+1).
numElems(M):-numZones(E),numSensors(F),M=E+F.

numUnits(N) :- comUnit(N), not comUnit(N+1).

%-------topological order---
startZone(1).
zoneDist(Z0,0):-startZone(Z0).

sensorDist(S,Dz+1):-zoneDist(Z,Dz),zone2sensor(Z,S),numElems(M),Dz<M.
zoneDist(Z,Ds+1):-sensorDist(S,Ds),zone2sensor(Z,S),numElems(M),Ds<M.

layer(z,Z,Dmin) :- zone(Z), zoneDist(Z,Dmin), not zoneDist(Z,Dmin-2).
layer(s,S,Dmin) :- sensor(S), sensorDist(S,Dmin), not sensorDist(S,Dmin-2).

layer(L) :- layer(_,_,L).
maxLayer(N) :- layer(N), not layer(N+1).