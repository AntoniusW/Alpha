% QP*-like heuristic that processes zones and sensors by layers, avoids assigning units that are already full and zones/sensors that are already assigned, and tries existing units in decreasing order first and then a fresh unit.
% Revised version of pup.alpha_heu_20210108.asp in which some things are simplified

elem(z,Z) :- zone2sensor(Z,D).
elem(s,D) :- zone2sensor(Z,D).

{ assign(U,T,X) } :- elem(T,X), comUnit(U).

#heuristic   assign(U,T,X) : comUnit(U), elem(T,X), maxLayer(ML), layer(T,X,L), not full(U,T), not T assigned(T,X), not T used(U), nUnits(NU), U=1, Level=ML-L. [ NU@Level ] % start with 1st unit
#heuristic   assign(U,T,X) : comUnit(U), elem(T,X), maxLayer(ML), layer(T,X,L), not full(U,T), not T assigned(T,X),     T used(U), Level=ML-L. [ U@Level ] % try preceding units in decreasing order
#heuristic   assign(U,T,X) : comUnit(U), elem(T,X), maxLayer(ML), layer(T,X,L), not full(U,T), not T assigned(T,X), not T used(U), comUnit(Um1), T used(Um1), F assign(Um1,T,X), Um1=U-1, Level=ML-L. [ U@Level ] % use the next fresh unit after all preceding units have been tried
#heuristic F assign(U,T,X) : comUnit(U), elem(T,X), not   assign(U,T,X), full(U,T), maxLayer(ML). [ 1@ML ] % do not use full units
#heuristic F assign(U,T,X) : comUnit(U), elem(T,X), not   assign(U,T,X). [1@-1] % close remaining choices

:- assign(U,T,X1), assign(U,T,X2), assign(U,T,X3), X1<X2, X2<X3.
:- assign(U1,T,X), assign(U2,T,X), U1<U2.
atLeastOneUnit(T,X):- assign(_,T,X).
:- elem(T,X), not atLeastOneUnit(T,X).

partnerunits(U,P) :- assign(U,z,Z), assign(P,s,D), zone2sensor(Z,D), U!=P.
partnerunits(U,P) :- partnerunits(P,U).
:- partnerunits(U,P1), partnerunits(U,P2), partnerunits(U,P3), P1<P2, P2<P3.

% helpers for QUICKPUP heuristics, inspired by quickpup.ascass by Erich Teppan:
sensor(S) :- zone2sensor(Z,S).
zone(Z) :- zone2sensor(Z,S).

numZones(N) :- zone(N), Np1=N+1, not zone(Np1).
numSensors(N) :- sensor(N), Np1=N+1, not sensor(Np1).
numElems(M):-numZones(E),numSensors(F),M=E+F.

numUnits(N) :- comUnit(N), Np1=N+1, not comUnit(Np1).

%-------topological order---
startZone(1).	% future work: other startZones (random choice?)
zoneDist(Z0,0) :- startZone(Z0).

sensorDist(S,Dz1) :- zoneDist(Z,Dz),zone2sensor(Z,S),numElems(M),Dz<M, Dz1=Dz+1.
zoneDist(Z,Ds1) :- sensorDist(S,Ds),zone2sensor(Z,S),numElems(M),Ds<M, Ds1=Ds+1.

zoneLayer(Z,Dmin) :- zone(Z), zoneDist(Z,Dmin), Dminm2=Dmin-2, not zoneDist(Z,Dminm2).
sensorLayer(S,Dmin) :- sensor(S), sensorDist(S,Dmin), Dminm2=Dmin-2, not sensorDist(S,Dminm2).

layer(L) :- zoneLayer(_,L).
layer(L) :- sensorLayer(_,L).
maxLayer(N) :- layer(N), Np1=N+1, not layer(Np1).

full(U,T) :- comUnit(U), assign(U,T,X1), assign(U,T,X2), X1<X2.
assigned(T,X) :- assign(_,T,X).
used(U) :- comUnit(U), assign(U,_,_).
% determine number of units (ATTENTION: this only works if units are numbered consecutively from 1):
nUnits(N) :- comUnit(N), Np1=N+1, not comUnit(Np1).

% translate old breadth-first syntax to new one:
layer(z,X,L) :- zoneLayer(X,L).
layer(s,X,L) :- sensorLayer(X,L).