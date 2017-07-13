package jason.asSyntax;

import jason.asSemantics.Unifier;
import jason.asSyntax.Conflict.ConflictType;
import jason.asSyntax.parser.as2j;

import java.io.Serializable;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Represents an AgentSpack plan
    (it extends structure to be used as a term)

 @navassoc - label - Pred
 @navassoc - event - Trigger
 @navassoc - context - LogicalFormula
 @navassoc - body - PlanBody
 @navassoc - source - SourceInfo

 */
public class Plan extends Structure implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Term TAtomic         = ASSyntax.createAtom("atomic");
    private static final Term TBreakPoint     = ASSyntax.createAtom("breakpoint");
    private static final Term TAllUnifs       = ASSyntax.createAtom("all_unifs");
    private static final String FConflict     = "conflict";

    private static Logger     logger          = Logger.getLogger(Plan.class.getName());

    private Pred              label  = null;
    private Trigger           tevent = null;
    private LogicalFormula    context;
    private PlanBody          body;

    private boolean isAtomic      = false;
    private boolean isAllUnifs    = false;
    private boolean hasBreakpoint = false;

    private boolean     isTerm = false; // it is true when the plan body is used as a term instead of an element of a plan

    //A set to store the conflicting plans related to the plan
    private Set<String> conflictingPlans = null;
    
    //A set to store the conflicts (labels, triggers, identifiers) related to the plan
    private Set<Conflict> conflicts = new HashSet<Conflict>();
    
    // used by clone
    public Plan() {
        super("plan", 0);
    }

    // used by parser
    public Plan(Pred label, Trigger te, LogicalFormula ct, PlanBody bd) {
        super("plan", 0);
        tevent = te;
        tevent.setAsTriggerTerm(false);
        setLabel(label);
        setContext(ct);
        if (bd == null) {
            body = new PlanBodyImpl();
        } else {
            body = bd;
            body.setAsBodyTerm(false);
        }
    }

    @Override
    public int getArity() {
        return 4;
    }

    private static final Term noLabelAtom = new Atom("nolabel");

    @Override
    public Term getTerm(int i) {
        switch (i) {
        case 0:
            return (label == null) ? noLabelAtom : label;
        case 1:
            return tevent;
        case 2:
            return (context == null) ? Literal.LTrue : context;
        case 3:
            return body;
        default:
            return null;
        }
    }

    @Override
    public void setTerm(int i, Term t) {
        switch (i) {
        case 0:
            label   = (Pred)t;
            break;
        case 1:
            tevent  = (Trigger)t;
            break;
        case 2:
            context = (LogicalFormula)t;
            break;
        case 3:
            body    = (PlanBody)t;
            break;
        }
    }

    public void setLabel(Pred p) {
        label = p;
        if (p != null && p.hasAnnot()) {
            for (Term t: label.getAnnots()) {
                if (t.equals(TAtomic)) {
                    //isAtomic = true; //TODO atomic as processConflictSet
                    //System.out.println("%%%%%%%%% Added atomic plan in conflict set of " + label.getFunctor());
                    this.conflicts.add(new Conflict("_", ConflictType.ATOMIC));
                } if (t.equals(TBreakPoint))
                    hasBreakpoint = true;
                if (t.equals(TAllUnifs))
                    isAllUnifs = true;
                if (t.isPred()) {
                    Pred pred = (Pred) t;
                    if (pred.getFunctor().equals(FConflict)) {
                        processConflictSet(pred);
                    }
                }
                // if change here, also change the clone()!
            }
        }
    }

    
    /**
     * Convert a term to a Conflict
     * @param termConflict
     * @return
     */
    private Conflict toConflict (Term termConflict) {
         Conflict c = null;
         if (termConflict.isAtom()) { //It's an identifier of a conflict
             c = new Conflict(((Atom)termConflict).getFunctor(), ConflictType.CONFLICT_IDENTIFIER);
         } else if (termConflict.isString()) { //It can be any type of conflict, depends on how it starts
             String s = ((StringTerm) termConflict).getString();
             if (s.charAt(0) == '@') {
                 c = new Conflict(s.substring(1, s.length()), ConflictType.PLAN_NAME);
             } else if (s.charAt(0) == '+' || s.charAt(0) == '-') {
                 c = new Conflict(s, ConflictType.PLAN_TRIGGER);
                 
                 Trigger t = Trigger.parseTrigger(s);
                 //System.out.println(s + "#" + t.toString());
             } else {
                 c = new Conflict(s, ConflictType.CONFLICT_IDENTIFIER);
             }
         } else if (termConflict.isUnnamedVar()) {
             c = new Conflict("_", ConflictType.ATOMIC);
             //System.out.println("%%%%%%%%%## _ Added atomic plan in conflict set of " + label.getFunctor());
         }
         return c;
    }
    
    /**
     * Create the conflict set of the plan
     * @param conflicts
     */
    private void processConflictSet(Pred conflicts) {
        if (conflicts.getArity() > 0) {
            if (conflicts.getTerm(0).isList()) {
                 for (Term termConflict: (ListTerm) conflicts.getTerm(0)) {
                     this.conflicts.add(toConflict(termConflict));
                 }
            } else {
                Term termConflict = conflicts.getTerm(0);
                this.conflicts.add(toConflict(termConflict));
            }
        }
    }
    
    public void addConflictingPlan(String label) {
    	if (conflictingPlans == null)
    		conflictingPlans = new HashSet<String>();
        conflictingPlans.add(label);
    }
    
    public void removeConflictingPlan(String label) {
        conflictingPlans.remove(label);
    }
    
    public boolean hasConflictingPlans() {
    	return conflictingPlans != null && !conflictingPlans.isEmpty();
    }
    public Set<String> getConflictingPlans() {
        return conflictingPlans;
    }
    
    public boolean containsConflict(Conflict c) {
        return conflicts.contains(c);
    }
    
    public Set<Conflict> getConflicts() {
        return conflicts;
    }
    
    public boolean conflictsWith(String label) {
        return conflictingPlans.contains(label);
    }
    
    public Pred getLabel() {
        return label;
    }

    public void setContext(LogicalFormula le) {
        context = le;
        if (Literal.LTrue.equals(le))
            context = null;
    }

    public void setAsPlanTerm(boolean b) {
        isTerm = b;
    }

    /** prefer using ASSyntax.parsePlan */
    public static Plan parse(String sPlan) {
        as2j parser = new as2j(new StringReader(sPlan));
        try {
            return parser.plan();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing plan " + sPlan, e);
            return null;
        }
    }

    /** @deprecated use getTrigger */
    public Trigger getTriggerEvent() {
        return tevent;
    }

    public Trigger getTrigger() {
        return tevent;
    }

    public LogicalFormula getContext() {
        return context;
    }

    public PlanBody getBody() {
        return body;
    }

    public boolean isAtomic() {
        return isAtomic;
    }

    public boolean hasBreakpoint() {
        return hasBreakpoint;
    }

    public boolean isAllUnifs() {
        return isAllUnifs;
    }

    /** returns an unifier if this plan is relevant for the event <i>te</i>,
        returns null otherwise.
    */
    public Unifier isRelevant(Trigger te) {
        // annots in plan's TE must be a subset of the ones in the event!
        // (see definition of Unifier.unifies for 2 Preds)
        Unifier u = new Unifier();
        if (u.unifiesNoUndo(tevent, te))
            return u;
        else
            return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (o != null && o instanceof Plan) {
            Plan p = (Plan) o;
            if (context == null && p.context != null) return false;
            if (context != null && p.context != null && !context.equals(p.context)) return false;
            return tevent.equals(p.tevent) && body.equals(p.body);
        }
        return false;
    }

    public Plan capply(Unifier u) {
        Plan p = new Plan();
        if (label != null) {
            p.label         = (Pred) label.capply(u);
            p.isAtomic      = isAtomic;
            p.hasBreakpoint = hasBreakpoint;
            p.isAllUnifs    = isAllUnifs;
        }
        
        for (Conflict c : conflicts) {
            p.conflicts.add(c.clone());
        }

        p.tevent = tevent.capply(u);
        if (context != null)
            p.context = (LogicalFormula)context.capply(u);
        p.body = (PlanBody)body.capply(u);
        p.setSrcInfo(srcInfo);
        p.isTerm = isTerm;

        return p;
    }

    public Term clone() {
        Plan p = new Plan();
        if (label != null) {
            p.label         = (Pred) label.clone();
            p.isAtomic      = isAtomic;
            p.hasBreakpoint = hasBreakpoint;
            p.isAllUnifs    = isAllUnifs;
        }
        
        for (Conflict c : conflicts) {
            p.conflicts.add(c.clone());
        }

        p.tevent = tevent.clone();
        if (context != null)
            p.context = (LogicalFormula)context.clone();
        p.body = body.clonePB();
        p.setSrcInfo(srcInfo);
        p.isTerm = isTerm;

        return p;
    }

    /** used to create a plan clone in a new IM */
    public Plan cloneOnlyBody() {
        Plan p = new Plan();
        if (label != null) {
            p.label         = label;
            p.isAtomic      = isAtomic;
            p.hasBreakpoint = hasBreakpoint;
            p.isAllUnifs    = isAllUnifs;
        }

        for (Conflict c : conflicts) {
            p.conflicts.add(c.clone());
        }
        
        p.tevent  = tevent.clone();
        p.context = context;
        p.body    = body.clonePB();

        p.setSrcInfo(srcInfo);
        p.isTerm = isTerm;

        return p;
    }

    public String toString() {
        return toASString();
    }

    /** returns this plan in a string complaint with AS syntax */
    public String toASString() {
        String b, e;
        if (isTerm) {
            b = "{ ";
            e = " }";
        } else {
            b = "";
            e = ".";
        }
        return b+((label == null) ? "" : "@" + label + " ") +
               tevent + ((context == null) ? "" : " : " + context) +
               (body.isEmptyBody() ? "" : " <- " + body) +
               e;
    }

    /** get as XML */
    public Element getAsDOM(Document document) {
        Element u = (Element) document.createElement("plan");
        if (label != null) {
            Element l = (Element) document.createElement("label");
            l.appendChild(new LiteralImpl(label).getAsDOM(document));
            u.appendChild(l);
        }
        u.appendChild(tevent.getAsDOM(document));

        if (context != null) {
            Element ec = (Element) document.createElement("context");
            ec.appendChild(context.getAsDOM(document));
            u.appendChild(ec);
        }

        if (!body.isEmptyBody()) {
            u.appendChild(body.getAsDOM(document));
        }

        return u;
    }
}
