package org.cs3.pl.tuprolog.internal;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import alice.tuprolog.Int;
import alice.tuprolog.InvalidTermException;
import alice.tuprolog.Library;
import alice.tuprolog.Number;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;

public class SWICompatibilityLibrary extends Library {
	
	
//	 A hashtable which stores the records of recorda,recordz, and recorded predicates.
	private Hashtable records_db = new Hashtable();

	// Temperary Hashtable used while evaluating structural equality.
	private Hashtable hsh = new Hashtable();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 *  Overloaded method to fix HashTable.containsKey since it checks the 
	 *  equality for both hashCode() and equals(). 
	 */
	private Term containsKey(Term key){
		Term _result = null;
		
		for (Iterator it = records_db.keySet().iterator(); it.hasNext();) {
			Term element = (Term) it.next();
			
			if ( key.equals(element)){
				_result = element;
				break;
			}
		}
		
		return _result;
		
	}

	/**
	 * method invoked when the engine is going
	 * to demonstrate a goal
	 */
	public void onSolveBegin(Term goal) {
		hsh = new Hashtable();
	}
	
	/**
	 * method invoked when the engine has
	 * finished a demostration
	 */
	public void onSolveEnd() {
		hsh = null;
	}
	

	/*
	 * 
	 * 	Implementation of Operators Extensions.
	 * 
	 * 
	 */		
	/**
	 * Structural Equality '=@=': TuProlog does not support structural
	 * equality yet.  
	 * 
	 * Two terms are structurally equal if  their tree representation is identical,
	 * and they  have the  same `pattern' of variables.
	 * 
	 * @param x The first term to be compared.
	 * @param y The second term to be compared.
	 * @return true if both terms are structurally equal, otherwise false.
	 */
	public boolean structEq_2(Term x,Term y){
		/*
		 * Extracts real Terms from TuProlog bindings.
		 * => a(b,c) compound is passed as X_e / a(b,c).
		 */
		Term first_term = x.getTerm();
		Term second_term = y.getTerm();
		
		if (first_term.isEqual(second_term))
			return true;

		if (first_term.isVar() && second_term.isVar())
			return true;
		
		if(first_term.isCompound() && second_term.isCompound()) {
			Struct frst =((Struct)first_term);
			Struct scnd =((Struct)second_term);
			/*
			 * checks the functor name
			 */
			if (!frst.getName().equals(scnd.getName()))
				return false;
			
			int arity = frst.getArity();
			/*
			 * checks the arity number
			 */
			if ( arity == scnd.getArity()){
			
				for (int i = 0; i < arity ; i++) {
					Term arg_1 = frst.getArg(i);
					Term arg_2 = scnd.getArg(i);

					/*
					 * Calls recursivelly structural equality between subterms.
					 */
					if (!structEq_2(arg_1 , arg_2))
						return false;
					/*
					 * Hashtable which stores the position of occurance for each 
					 * Var subterm.
					 */
					if (arg_1.isVar())
						if (hsh.containsKey(arg_1) || hsh.containsValue(arg_2)){
							if (((Term)hsh.get(arg_1)) != arg_2)
								return false;
						} else
							hsh.put(arg_1, arg_2);
						
				}
				
				/*
				 * All subterms are structurally equal.
				 */
				return true;				
			}
			
			/*
			 * differenet arities.
			 */
			return false;
		}
		
		/*
		 *  The rest are false
		 */
		return false;
	}
	

	
	/*
	 * 
	 * 	Implementation of Exceptions Handling.
	 * 	- throw/1
	 * 	- catch/3
	 * 
	 */
	
	/**
	 * TuProlog does not support Exceptions yet.
	 * 
	 * throw(Exception): 
	 * 	Raise  an exception.
	 * 
	 * @param exception An exception to be thrown.
	 * @return
	 * @throws Exception
	 * @throws TuPrologThrowable
	 */
	public boolean throw_1(Term exception) throws Exception, TuPrologThrowable {
		/*
		 * TODO: throw the term, then try to unify it instead of simple string case. 
		 */
		throw new TuPrologThrowable( exception.toString(), exception.getTerm());
	}
	
	/**
	 * TuProlog does not support Exceptions yet.
	 * 
	 * catch(goal, catcher, recover): 
	 * 	Catch an exception thrown by throw/1 predicate.
	 * 
	 * @param goal A goal to execute.
	 * @param catcher An exception expected to be caught.
	 * @param recover A goal to execute in case of an exception.
	 * @return
	 */
	public boolean catch_3(Term goal, Term catcher, Term recover){
		
		try{
			engine.solve(goal);
		}catch(TuPrologThrowable ex){
/*
			if ( ex.getMessage().contentEquals( new StringBuffer(catcher.getTerm().toString()) ))
				engine.solve(recover);
			else
				throw ex;
*/
			boolean unify = unify(ex.getExceptionTerm(), catcher);
		
			if (unify)
				return (engine.solve(recover)).isSuccess();
		}
		return true;
	}


	
	
	/*
	 * 
	 * 	Implementation of Recorded DB.
	 * 	- recorda/2
	 * 	- recorda/3
	 * 	- recordz/2
	 * 	- recordz/3
	 * 	- recorded/2
	 * 	- recorded/3 
	 * 
	 */		
	//FIXME record references always starts from one, it needs to be more random.
	private String recorded_theory = "record_ref(1). \n" +
			"recorda(Key, Value, Ref):- " +
//			"	format('-------recorda---- ~w ~w ~w~n', [Key, Value, Ref])," +
			"	var(Ref), record_ref(CurrRef), Ref is CurrRef+1," +
			"	asserta(record_db(Key, Value, Ref)), " +
			"	update_ref. \n" +
			"recordz(Key, Value, Ref):- " +
//			"	format('-------recordz---- ~w ~w ~w~n', [Key, Value, Ref])," +			
			"	var(Ref), record_ref(CurrRef), Ref is CurrRef+1," +
			"	assertz(record_db(Key, Value, Ref)), " +
			"	update_ref. \n" +
			"recorded(Key, Value, Ref):-" +
//			"	format('-------recorded---- ~w ~w ~w~n', [Key, Value, Ref])," +			
			"	record_db(Key, Value, Ref).\n" +
			"erase(Ref):-" +
//			"	format('-------erase---- ~w ~n',  Ref)," +			
			"	nonvar(Ref), retract(record_db(_, _, Ref)).\n" +
			"recordz(Key, Value):-" +
			"	recordz(Key, Value, _). \n" +
			"recorda(Key, Value):-" +
			"	recorda(Key, Value, _). \n" +
			"recorded(Key, Value):-" +
			"	recorded(Key, Value, _). \n" +
			"update_ref:-" +
//			"	write('-------update_ref---- ')," +
			"	record_ref(CurrRef), retract(record_ref(_))," +
			"	NxtRef is CurrRef+1, assert(record_ref(NxtRef)).\n" ;
	
//	
//	
//	/**
//	 * TODO: The current implementation is not efficient, it needs to be optimized.
//	 */
//
//	/**
//	 * recorda(+Key, +Term) :
//	 * 	Equivalent to recorda(Key, Value,  _).
//	 * 
//	 * @param key A key to store values under.
//	 * @param value A value to be stored under key.
//	 * @return
//	 */
//	public boolean recorda_2(Term key, Term value){
//		Term ref = new Var();
//		
//		return ( recorda_3(key, value, ref) );
//	}	
//
//	/**
//	 * recorda(+Key, +Term, -Reference) :
//	 * 	Assert  Term  in the  recorded  database under key  Key. Key  is an  integer,
//	 *  atom or  term. Reference  is unified  with a  unique reference to the record
//	 *  (see erase/1).
//	 *   
//	 * @param key	A key to store values under.
//	 * @param value	 A value to be stored under key.
//	 * @param ref	A unique integer used as reference to the value stored.
//	 * @return
//	 */
//	public boolean recorda_3(Term key, Term value, Term ref){
//		Term _key = key.getTerm();
//		Term _value = value.getTerm();
//		
//		/*
//		 * Key & Value should be bound to store a record
//		 */
//		if ( _key.isVar() || _value.isVar() )
//			return false;
//		
//		Term tmp = containsKey(_key);
//		List values = null ;
//		
//		if ( tmp == null )
//		{
//			values = new ArrayList();
//			tmp = _key;
//		}else 
//			values = (ArrayList) records_db.get(tmp);
//				
//		RecordEntry entry = new RecordEntry(_value);
//		values.add(0, entry);
//		records_db.put(tmp, values);
//		
//		return unify(ref, new Int(entry.getRef()));
//	}
//	
//	/**
//	 * recordz(+Key, +Term):
//	 * 	Equivalent to recordz(Key, Value,  _).
//	 * 
//	 * @param key A key to store values under.
//	 * @param value A value to be stored under key.
//	 * @return
//	 */
//	public boolean recordz_2(Term key, Term value){
//		Term ref = new Var();
//		
//		return ( recordz_3(key, value, ref) );
//	}	
//	
//	/**
//	 * recordz(+Key, +Term, -Reference):
//	 * 	Equivalent to recorda/3, but  puts the Term at the tail of the terms
//	 *  recorded under Key.
//	 *  
//	 * @param key	A key to store values under.
//	 * @param value	 A value to be stored under key.
//	 * @param ref	A unique integer used as reference to the value stored.
//	 * @return
//	 */
//	public boolean recordz_3(Term key, Term value, Term ref){
//		Term _key = key.getTerm();
//		Term _value = value.getTerm();
//		
//		/*
//		 * Key & Value should be bound to store a record
//		 */
//		if ( _key.isVar() || _value.isVar() )
//			return false;
//		
//		Term tmp = containsKey(_key);
//		List values = null ;
//		
//		if ( tmp == null )
//		{
//			values = new ArrayList();
//			tmp = _key;
//		}else 
//			values = (ArrayList) records_db.get(tmp);
//		
//		RecordEntry entry = new RecordEntry(_value);
//		values.add(entry);
//		records_db.put(tmp, values);
//		
//		return unify(ref, new Int(entry.getRef()));		
//	}
//	
//	/**
//	 * recorded(+Key, -Value):
//	 * 	Equivalent to recorded(Key, Value,  _).
//	 * 
//	 * @param key A key to store values under.
//	 * @param value A value to be stored under key.
//	 * @return
//	 */
//	public boolean recorded_2(Term key, Term value){
//		Term ref = new Var();
//		return ( recorded_3(key, value, ref) );
//	}	
//	
//	/**
//	 * recorded(+Key, -Value, -Reference):
//	 * 	Unify  Value  with the  first  term recorded  under Key  which  does
//	 *  unify.    Reference  is  unified with  the  memory location  of  the
//	 *  record.
//	 *  
//	 * @param key	A key to store values under.
//	 * @param value	 A value to be stored under key.
//	 * @param ref	A unique integer used as reference to the value stored.
//	 * @return
//	 */
//	public boolean recorded_3(Term key, Term value, Term ref){
//		Term _key = key.getTerm();
//		Term _value = value.getTerm();
//
//		// Key should be bound
//		if ( _key.isVar() )
//			return false;
//		
//		Term tmp = containsKey(_key);
//		
//		if ( tmp == null )
//			return false;
//
//		List values = (ArrayList)records_db.get(tmp);
//		
//		
//		int index = -1;
//		
//		//FIXME in case of multiple solutions only the first one is unified.
//		for (Iterator it = values.iterator(); it.hasNext();) {
//			RecordEntry en = (RecordEntry) it.next();
//			//if (en.getTerm().equals(_value))
//			
//			if(unify(_value, en.getTerm()))
//				index = en.getRef();
//		}
//		// Value was not found.
//		if ( index == -1 )
//			if ( _value.isVar() ) {
//				// TODO: incase of multiple values return a list.
//				unify(value, ((RecordEntry)values.get(0)).getTerm());
//				boolean result = unify(ref, new Int(((RecordEntry)values.get(0)).getRef()));
//				return result;
//			}else
//				return false;
//
//		boolean result = unify( ref, new Int(index) );
//		return result;
//	}
//	
//	/**
//	 * erase(+Reference):
//	 * 	Erase  a  record or  clause from  recorded  database. Reference  is  an
//	 *  integer  returned by  recorda/3 or  recorded/3. Erase can only be called
//	 *  once on  a record or clause. 
//	 *  
//	 * @param ref	A unique integer used as reference to a value stored in recorded_db.
//	 * @return
//	 */
//	public boolean erase_1(Term ref){
//		
//		if ( ! ref.getTerm().isNumber() )
//			return false;
//		
//		Number _ref = (Number) ref.getTerm() ;		
//		
//		for (Iterator keys = records_db.keySet().iterator(); keys.hasNext();) {
//			Term key = (Term) keys.next();
//			
//			for (Iterator values = ((ArrayList)records_db.get(key)).iterator(); values.hasNext();) {
//				RecordEntry en = (RecordEntry) values.next();
//				
//				if (en.getRef() == _ref.intValue()){
//					values.remove();
//					return true;
//				}
//			}
//		}
//		
//		return false;
//	}	
	
	/*
	 * 
	 * 	Implemenation of MultiThreading .
	 * 	- with_mutex/2
	 *  
	 */		
	
	/**
	 * A hashtable which stores all synchornization keys.
	 */
	private Hashtable monitors = new Hashtable();

	/**
	 * 
	 * @param key key to synchronize on.
	 * @param goal
	 * @return
	 */
	public boolean with_mutex_2(Struct key, Term goal) {
		
		Object monitor = monitors.get(key.getName());

		if(monitor == null) {
			monitor = new Object();
			monitors.put(key.getName(), monitor);
		}
		synchronized (monitor) {
			SolveInfo info = this.getEngine().solve(goal);
			return (info.isSuccess())?true:false;
		}
	}

	/*
	 * 
	 * 	Implemenation of Formations .
	 * 	- format/2
	 *  - sformat/3
	 * 
	 */		
	
	public boolean format_2(Term msg, Term values){
		sformat_3(null , msg, values);
		return true;
	}
	
	public boolean sformat_3(Term string_name, Term msg, Term values){
		//TODO: implement format.
		String result = msg.toString();
		result = result.replace('\'', ' ');

		String output= "";
		int old_indx = 0;
		int indx = result.indexOf('~');
		Term temp_values = values.getTerm();		
		
		while(indx != -1 ){
			
			output += result.substring(old_indx, indx);

			switch(result.charAt(indx+1)){
				case 'w':{
				
					if ( temp_values.isList() ){
						Struct vals = (Struct) temp_values.getTerm();
						output += vals.getArg(0).getTerm();
						//if (cnt < vals.getArity()-1)
						temp_values = vals.getArg(1);
					}else
						output += values.getTerm();
					
				}break;
				case 'n':{
					output += '\n';
				}break;
				
			}

			old_indx = indx + 2;
			indx = result.indexOf('~', indx+1);
		}
		
		output += result.substring(old_indx);
		//System.out.println("Pattern :"+ result);
		
		if (string_name != null){
			boolean unified = false;
			try {
				unified = unify(string_name, Term.parse(output));
	
			} catch (InvalidTermException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	
			return unified;
		}
		
		//TODO: move this functionality to format/2 instead of relying on null input for string name
		engine.stdOutput(output);	
		return false;		
	}

	/*
	 * 
	 * Implementation of term_to_atom.
	 * - term_to_atom/1
	 * 
	 */
	
	public boolean term_to_atom_2(Term term, Term atom) {
		
		return unify(term, atom);
		
	}
	
	/*
	 * 	Implemenation of the SessionId .
	 */
	public boolean session_self_1(Term sessionId){
		/*
		 * FIXME: for the current moment, I am using current session hashCode as sessionID. 
		 * dirty solution.
		 */
		return unify(sessionId, new Int(TuPrologPrologInterface.currentActiveSession) );
	}
	
	/*
	 * 
	 * 	Implemenation of the default theory .
	 * 
	 * 
	 */		
	

	/**
	 * The default Theory which will be used by SWICompatibilityLibrary once loaded.
	 * @see alice.tuprolog.Library#getTheory()
	 */
	public String getTheory(){
		 return 
		 	":-	op( 700, xfx, '=@=').				\n" +
	 		":-	op( 700, xfx, '\\=@=').				\n" +
	 		"'=@='(X,Y):-  structEq(X,Y).			\n" +
	 		"'\\=@='(X,Y):-  not structEq(X,Y). 	\n" +
	 		"forall(A,B):- findall(_,(A,B), _). \n"+
	 		"forall-call(A,B):- A, B. 				\n"+
	 		"':'(Module,Predicate) :- call(Predicate).	\n" 
		   +		recorded_theory;
	}
}
