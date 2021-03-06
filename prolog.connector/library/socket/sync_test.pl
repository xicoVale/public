/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * Author: Lukas Degener (among others)
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2004-2012, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 ****************************************************************************/

:- multifile test/1.
:- multifile tearDown/1.

:- [magicmap].
:- [sync].
:- [localisation].

test(separate_functor_arity_module_safe):-
    sync:separate_functor_arity_module_safe(m:f/1, m:f, 1),
    sync:separate_functor_arity_module_safe('m:f/1', m:f, 1),
    sync:separate_functor_arity_module_safe(f/2, f, 2).
    
test(evaluate_and_store_idb):-
    sync:evaluate_and_store_idb(mymodule:non_existing(_T)),
    clause(sync:idb(mymodule:non_existing(_A)),fail).

tearDown(evaluate_and_store_idb):-
    retractall(sync:idb(_)).


test(update_idb_and_changed):-
    Term = testmodule:newpred(_A,b),
    term_to_atom(Term, Atom),
    sync:init_idb(Atom),
    assert(testmodule:newpred(a,b)),
    sync:term_ref(Term,Ref),
	sync:update_idb(Term, Ref),
	testmodule:newpred(a,b),
    clause(sync:idb_copy(Ref,testmodule:newpred(_,_)),fail),
    sync:changed(Ref).

tearDown(update_idb_and_changed):-
	sync:term_ref(testmodule:newpred(_A,b), Ref),
	sync:remove_term_idb(Ref),
	retractall(testmodule:newpred(_,_)).
    
tearDown('update_idb and changed'):-
    retractall(sync:idb(_)),
    retractall(sync:idb_copy(_)),
    retractall(testmodule:newpred(_,_)).

setUp(term_ref) :-
    sync:init_term_ref(term(a,_B),_Ref).
    	
test(term_ref):-
    assert_true(sync:term_ref(term(a,_C),_Ref)).
    
tearDown(term_ref):-
	sync:term_ref(term(a,_C),Ref),
	sync:remove_term_idb(Ref).

setUp(init_idb):-
    assert(magicmap:location('00-09-2D-53-27-3A', 50.73, 7.12, 0)),
    retractall(sync:idb(_,_)).
    
test(init_idb) :-
  sync:init_idb('localisation:company_nearby(_,_,_,1000)'),
  sync:term_ref(localisation:company_nearby(_,_,_,1000), Ref),


  retract(magicmap:location('00-09-2D-53-27-3A', 50.73, 7.12, 0)),
  assert(magicmap:location('00-09-2D-53-27-3A', 50.73, 7.123, 0)),

  sync:update_idb(localisation:company_nearby(_,_,_,1000), Ref),

  sync:changed(Ref).
  
tearDown(init_idb):-
    retractall(magicmap:location(_,_,_,_)),
    sync:term_ref(localisation:company_nearby(_,_,_,1000), Ref),
    sync:remove_term_idb(Ref).
 


