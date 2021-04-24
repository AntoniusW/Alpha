% A very simple example, how to encode object oriented models in ASP
% we have n possible objects (which are of a specific class or not used at all)
% an object is identified by an integer
% in this example there a 3 classes(bicycle, tricycle and wheel), 1 association and 1 attribute
% association vehicle(V,W) 
% a bicycle must have 2 wheels, a tricycle must have 3 wheels, 
% a wheel must have a vehicle (i.e. bicycle or tricycle) assigned
% attribute: wheel_size wheels have sizes (small,medium,large)
% 1 constraint: the wheels belonging to the same vehicle must have the same size

%#const n = 5.
%object(1..n).
object(1).
object(2).
object(3).
object(4).
object(5).

% restrict search space
%object_type(1,unused).
%object_type(2,unused).
%object_type(3,unused).
%object_type(4,unused).
%object_type(5,unused).



type(wheel).
type(bicycle).
type(tricycle).
type(unused).
size(small).
size(medium).
size(large).

%%0 { object_type(O,T):type(T)} 1 :- object(O).
%object_type(O,unused)	:- object(O), not object_type(O,bicycle),	not object_type(O,tricycle),	not object_type(O,wheel).
%object_type(O,wheel)	:- object(O), not object_type(O,bicycle),	not object_type(O,tricycle),	not object_type(O,unused).
%object_type(O,bicycle)	:- object(O), not object_type(O,wheel),		not object_type(O,tricycle),	not object_type(O,unused).
%object_type(O,tricycle)	:- object(O), not object_type(O,bicycle),	not object_type(O,wheel),		not object_type(O,unused).

% adoptions to use unique-rule head constraints
% first, make each rule head unique
object_type1(O,unused)	:- object(O), not object_type3(O,bicycle),	not object_type4(O,tricycle),	not object_type2(O,wheel).
object_type2(O,wheel)	:- object(O), not object_type3(O,bicycle),	not object_type4(O,tricycle),	not object_type1(O,unused).
object_type3(O,bicycle)	:- object(O), not object_type2(O,wheel),		not object_type4(O,tricycle),	not object_type1(O,unused).
object_type4(O,tricycle)	:- object(O), not object_type3(O,bicycle),	not object_type2(O,wheel),		not object_type1(O,unused).
:- object(X), not object_type1(X,unused), not object_type2(X,wheel), not object_type3(X,bicycle), not object_type4(X,tricycle).
% redirect to object_type/2
object_type(X,T) :- object_type1(X,T).
object_type(X,T) :- object_type2(X,T).
object_type(X,T) :- object_type3(X,T).
object_type(X,T) :- object_type4(X,T).
:- vehicle(V), not object_type(V,bicycle), not object_type(V,tricycle).

% assign wheels to vehicles
%1 { vehicle_wheel(O,W):T!=wheel,object_type(O,T) } 1 :- object_type(W,wheel).
vehicle(V) :- object_type(V,bicycle).
vehicle(V) :- object_type(V,tricycle).


vehicle_wheel(V,W) :- object_type(W,wheel), vehicle(V), not not_vehicle_wheel(V,W).
not_vehicle_wheel(V,W) :- object_type(W,wheel), vehicle(V), not vehicle_wheel(V,W).
:- object_type(W,wheel), vehicle_wheel(V1,W), vehicle_wheel(V2,W), V1 != V2.
wheel_has_vehicle(W) :- vehicle_wheel(X_,W).
:- object_type(W,wheel), not wheel_has_vehicle(W).

% wheel size
%1 { wheel_size(W,S):size(S) } 1 :- object_type(W,wheel).
wheel_size(W,S) 	:- object_type(W,wheel), size(S), not not_wheel_size(W,S).
not_wheel_size(W,S)	:- object_type(W,wheel), size(S), not wheel_size(W,S).
:- object_type(W,wheel), wheel_size(W,S1), wheel_size(W,S2), S1 != S2.
wheel_has_size(W) :- wheel_size(W,X_).
:- object_type(W,wheel), not wheel_has_size(W).

% cardinality restrictions for vehicle_wheel
%vehicle_has_wheel(V) :- vehicle(V), vehicle_wheel(V,_).
vehicle_has_2_wheels(V) :- vehicle(V), vehicle_wheel(V,W1), vehicle_wheel(V,W2), W1 != W2.
vehicle_has_3_wheels(V) :- vehicle(V), vehicle_wheel(V,W1), vehicle_wheel(V,W2), vehicle_wheel(V,W3), W1 != W2, W1 != W3, W2 != W3.
:- object_type(B,bicycle), not vehicle_has_2_wheels(B).
:- object_type(B,bicycle), vehicle_wheel(B,W1), vehicle_wheel(B,W2), vehicle_wheel(B,W3), W1 != W2, W1 != W3, W2 != W3.
:- object_type(T,tricycle), not vehicle_has_3_wheels(T).
:- object_type(T,tricycle), vehicle_wheel(B,W1), vehicle_wheel(B,W2), vehicle_wheel(B,W3), vehicle_wheel(B,W4), W1 != W2, W1 != W3, W1 != W4, W2 != W3, W2 != W4, W3 != W4.

% constraint wheel size
:-wheel_size(W1,S1),wheel_size(W2,S2),vehicle_wheel(V,W1),vehicle_wheel(V,W2),S1!=S2.