/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2004-2012, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 ****************************************************************************/

:- module(edge_counter,[count_call_edges_between_predicates/0,
						call_edges_for_predicates/3]).

:- ensure_loaded('../pdt_factbase').

:- dynamic call_edges_for_predicates/3. %call_edges_for_predicates(SourceID,TargetID,Counter)
count_call_edges_between_predicates:-
    retractall(call_edges_for_predicates(_,_,_)),
	forall(	call_edge(TargetId, SourceLiteralId),
			(	(	literalT(SourceLiteralId,_,SourceRule,_,_,_),
    				pred_edge(SourceRule,SourceId)
   				)
			->	inc_call_edges_for_predicates(SourceId,TargetId)
			;	(	predicateT(TargetId,_,TFunctor,TArity,TModule),
					format('Problem with call-edge: ~w -> ~w (~w:~w/~w)~n',[SourceLiteralId, TargetId, TModule, TFunctor, TArity])
				)
			)
		).	

inc_call_edges_for_predicates(SourceID,TargetID):-
    call_edges_for_predicates(SourceID,TargetID,Counter),
	retract(call_edges_for_predicates(SourceID,TargetID,Counter)),
 	New_Counter is (Counter + 1),
    assert(call_edges_for_predicates(SourceID,TargetID,New_Counter)).
inc_call_edges_for_predicates(SourceID,TargetID):-  
    assert(call_edges_for_predicates(SourceID,TargetID,1)).


