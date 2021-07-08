% this version contains two minor corrections to house_alpha_2020-02-21.asp

% INITIAL PROBLEM
cabinetDomain(C) :- cabinetDomainNew(C).
roomDomain(R) :- roomDomainNew(R).

% guessing of cabinets and rooms in a configuration
{ cabinet(C) } :- cabinetDomain(C).
{ room(R) } :- roomDomain(R).

% ordering of used cabinets and rooms
room(R1) :- roomDomainNew(R1), roomDomainNew(R2), room(R2), R1 < R2.
cabinet(C1) :- cabinetDomainNew(C1), cabinetDomainNew(C2), cabinet(C2), C1 < C2.

% association cabinetTOthing
% 1 <= {cabinetTOthing(C,T) : cabinetDomain(C)} <= 1 :- thing(T).
{ cabinetTOthing(C,T) } :- cabinetDomain(C), thing(T).
thingHasCabinet(T) :- cabinetTOthing(C,T).
:- thing(T), not thingHasCabinet(T).
:- thing(T), cabinetDomain(C1), cabinetTOthing(C1,T), cabinetDomain(C2), cabinetTOthing(C2,T), C1 < C2.
:- 6 <= #count { T : cabinetTOthing(C,T), thing(T) }, cabinet(C).

% association roomTOcabinet
% 1 <= {roomTOcabinet(R,C) : roomDomain(R)} <= 1 :- cabinet(C).
{ roomTOcabinet(R,C) } :- roomDomain(R), cabinet(C).
cabinetHasRoom(C) :- roomTOcabinet(R,C).
:- cabinet(C), not cabinetHasRoom(C).
:- cabinet(C), roomDomain(R1), roomTOcabinet(R1,C), roomDomain(R2), roomTOcabinet(R2,C), R1 < R2.
:- 5 <= #count { C : roomTOcabinet(R,C), cabinetDomain(C) }, room(R).

% association personTOroom
% a room belongs to a person, who stores things in cabinets in that room
personTOroom(P,R) :- personTOthing(P,T), cabinetTOthing(C,T), roomTOcabinet(R,C).
legacyConfig(personTOroom(P,R)) :- legacyConfig(personTOthing(P,T)), legacyConfig(cabinetTOthing(C,T)), legacyConfig(roomTOcabinet(R,C)).

% things of one person cannot be placed in a cabinet together with things of another person
:- cabinetTOthing(C,T1), cabinetTOthing(C,T2),
   personTOthing(P1,T1), personTOthing(P2,T2), P1 < P2.

% cabinets of different people cannot be placed in the same room
% i.e. one and the same room cannot belong to 2 different persons
:- personTOroom(P1,R), personTOroom(P2,R), P1 < P2.

room(R) :- roomTOcabinet(R,C).
room(R) :- personTOroom(P,R).

cabinet(C) :- cabinetTOthing(C,T).
cabinet(C) :- roomTOcabinet(R,C).

% MODIFIED PROBLEM
% there are 2 types of things which are disjoint
thing(T) :- thingLong(T).
thing(T) :- thingShort(T).
thingShort(T) :- thing(T), not thingLong(T).
:- thingLong(T), thingShort(T).

% long things have to be packed in high cabinets
% 1 <= {cabinetHigh(C); cabinetSmall(C)} <= 1 :- cabinet(C).
cabinetHigh(C) :- cabinet(C), not cabinetSmall(C).
cabinetSmall(C) :- cabinet(C), not cabinetHigh(C).
:- cabinet(C), cabinetHigh(C), cabinetSmall(C).
cabinetHigh(C) :- thingLong(T), cabinetTOthing(C,T).

% at most either 2 high cabinets or 1 high and 2 short or 4 short cabinets
% are allowed to be in a room
% assumption : there are 4 cabinet slots in each room.
% High cabinet requires 2 slots, small - 1 slot
cabinetSize(C,1) :- cabinet(C), cabinetSmall(C).
cabinetSize(C,2) :- cabinet(C), cabinetHigh(C).
roomTOcabinetSize(R,C,S) :- roomTOcabinet(R,C), cabinetSize(C,S).
:- 5 <= #sum { S,C : roomTOcabinetSize(R,C,S), cabinetDomain(C) }, room(R).

% TRANSFORMATION RULES
person(P) :- legacyConfig(person(P)).
thing(T) :- legacyConfig(thing(T)).
personTOthing(P,T) :- legacyConfig(personTOthing(P,T)).

% 1 <= {reuse(cabinetTOthing(C,T)); delete(cabinetTOthing(C,T))} <= 1 :- legacyConfig(cabinetTOthing(C,T)).
reuse(cabinetTOthing(C,T))  :- legacyConfig(cabinetTOthing(C,T)), not delete(cabinetTOthing(C,T)).
delete(cabinetTOthing(C,T)) :- legacyConfig(cabinetTOthing(C,T)), not  reuse(cabinetTOthing(C,T)).

% 1 <= {reuse(roomTOcabinet(R,C));  delete(roomTOcabinet(R,C)) } <= 1 :- legacyConfig(roomTOcabinet(R,C)).
reuse(roomTOcabinet(R,C))  :- legacyConfig(roomTOcabinet(R,C)), not delete(roomTOcabinet(R,C)).
delete(roomTOcabinet(R,C)) :- legacyConfig(roomTOcabinet(R,C)), not  reuse(roomTOcabinet(R,C)).

% 1 <= {reuse(personTOroom(P,R));   delete(personTOroom(P,R))  } <= 1 :- legacyConfig(personTOroom(P,R)).
reuse(personTOroom(P,R))  :- legacyConfig(personTOroom(P,R)), not delete(personTOroom(P,R)).
delete(personTOroom(P,R)) :- legacyConfig(personTOroom(P,R)), not  reuse(personTOroom(P,R)).

% all cabinets from the original solution are added to the new domain
cabinetDomain(C) :- legacyConfig(cabinet(C)).
% 1 <= {reuse(cabinet(C)); delete(cabinet(C))} <= 1 :- legacyConfig(cabinet(C)).
reuse(cabinet(C))  :- legacyConfig(cabinet(C)), not delete(cabinet(C)).
delete(cabinet(C)) :- legacyConfig(cabinet(C)), not  reuse(cabinet(C)).

% all rooms from the original solution are added to the new domain
roomDomain(R) :- legacyConfig(room(R)).
% 1 <= {reuse(room(R)); delete(room(R))} <= 1 :- legacyConfig(room(R)).
reuse(room(R))  :- legacyConfig(room(R)), not delete(room(R)).
delete(room(R)) :- legacyConfig(room(R)), not  reuse(room(R)).

% all reused atoms should be a part of a reconfiguration
% i.e. they should be defined as facts
cabinetTOthing(C,T) :- reuse(cabinetTOthing(C,T)).
roomTOcabinet(R,C) :- reuse(roomTOcabinet(R,C)).
personTOroom(P,R) :- reuse(personTOroom(P,R)).
cabinet(C) :- reuse(cabinet(C)).
room(R) :- reuse(room(R)).

% all deleted atoms cannot be used in a configuration
:- cabinetTOthing(C,T), delete(cabinetTOthing(C,T)).
:- roomTOcabinet(R,C), delete(roomTOcabinet(R,C)).
:- personTOroom(P,R), delete(personTOroom(P,R)).
:- cabinet(C), delete(cabinet(C)).
:- room(R), delete(room(R)).

% CONSTRUCTIVE HEURISTICS
maxCabinet(C) :- cabinetDomainNew(C), not cabinetDomainNew(C+1).
maxRoom(R)    :-    roomDomainNew(R), not    roomDomainNew(R+1).
fullCabinet(C) :- 5 <= #count { T : cabinetTOthing(C,T), thing(T) }, cabinet(C).
fullRoom(R) :- room(R), 4 <= #sum { S,C : roomTOcabinetSize(R,C,S), cabinetDomain(C) }.
assignedThing(T) :- cabinetTOthing(_,T).
assignedCabinet(C) :- roomTOcabinet(_,C).
personTOcabinet(P,C) :- personTOthing(P,T), cabinetTOthing(C,T).
personTOroom(P,R)    :- roomTOcabinet(R,C), personTOcabinet(P,C).
otherPersonTOcabinet(P,C) :- personTOcabinet(P2,C), person(P), P!=P2.
otherPersonTOroom(P,R)    :- roomTOcabinet(R,C), personTOcabinet(P2,C), person(P), P!=P2.
% first try to reuse legacy config (assigning long things before short things):
#heuristic reuse(cabinet(C))  : legacyConfig(cabinet(C)). [6@4]
#heuristic reuse(room(R))     : legacyConfig(room(R)). [5@4]
#heuristic reuse(cabinetTOthing(C,T)) : legacyConfig(cabinetTOthing(C,T)),     thingLong(T). [4@4]
#heuristic reuse(cabinetTOthing(C,T)) : legacyConfig(cabinetTOthing(C,T)), not thingLong(T). [3@4]
#heuristic reuse(roomTOcabinet(R,C))  : legacyConfig(roomTOcabinet(R,C)). [2@4]
#heuristic reuse(personTOroom(P,R))  : legacyConfig(personTOroom(P,R)). [1@4]
% then fill cabinets with things (assigning long things before short things):
#heuristic cabinetTOthing(C,T) : cabinetDomain(C), not fullCabinet(C), not T assignedThing(T), personTOthing(P,T), not otherPersonTOcabinet(P,C), maxCabinet(MC), W=MC-C,     thingLong(T). [W@3]
#heuristic cabinetTOthing(C,T) : cabinetDomain(C), not fullCabinet(C), not T assignedThing(T), personTOthing(P,T), not otherPersonTOcabinet(P,C), maxCabinet(MC), W=MC-C, not thingLong(T). [W@2]
% then fill rooms with cabinets:
#heuristic roomTOcabinet(R,C)  : roomDomain(R), not fullRoom(R), cabinet(C), not T assignedCabinet(C), personTOcabinet(P,C), not otherPersonTOroom(P,R), maxRoom(MR), W=MR-R. [W@1]
% when all things and cabinets are assigned, close remaining choices:
#heuristic F cabinet(C) : not cabinet(C), cabinetDomain(C).
#heuristic F room(R) : not room(R), roomDomain(R).
#heuristic F cabinetTOthing(C,T) : not cabinetTOthing(C,T), cabinetDomain(C), thing(T).
#heuristic F roomTOcabinet(R,C) : not roomTOcabinet(R,C), roomDomain(R), cabinet(C).