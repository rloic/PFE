package com.github.rloic.wip;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.variables.delta.IDelta;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.view.IView;
import org.chocosolver.util.ESat;
import org.chocosolver.util.iterators.DisposableRangeIterator;
import org.chocosolver.util.iterators.DisposableValueIterator;
import org.chocosolver.util.iterators.EvtScheduler;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableSet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;

public class BoolVarView implements BoolVar {

   private final BoolVar delegate;

   public BoolVarView(BoolVar of) {
      this.delegate = of;
   }

   @Override
   public ESat getBooleanValue() {
      return delegate.getBooleanValue();
   }

   @Override
   public boolean setToTrue(ICause cause) throws ContradictionException {
      return delegate.setToTrue(cause);
   }

   @Override
   public boolean setToFalse(ICause cause) throws ContradictionException {
      return delegate.setToFalse(cause);
   }

   @Override
   public BoolVar not() {
      return delegate.not();
   }

   @Override
   public boolean hasNot() {
      return delegate.hasNot();
   }

   @Override
   public void _setNot(BoolVar not) {
      delegate._setNot(not);
   }

   @Override
   public boolean isLit() {
      return delegate.isLit();
   }

   @Override
   public boolean isNot() {
      return delegate.isNot();
   }

   @Override
   public void setNot(boolean isNot) {
      delegate.setNot(isNot);
   }

   @Override
   public boolean removeValue(int value, ICause cause) throws ContradictionException {
      return delegate.removeValue(value, cause);
   }

   @Override
   public boolean removeValues(IntIterableSet values, ICause cause) throws ContradictionException {
      return delegate.removeValues(values, cause);
   }

   @Override
   public boolean removeAllValuesBut(IntIterableSet values, ICause cause) throws ContradictionException {
      return delegate.removeAllValuesBut(values, cause);
   }

   @Override
   public boolean removeInterval(int from, int to, ICause cause) throws ContradictionException {
      return delegate.removeInterval(from, to, cause);
   }

   @Override
   public boolean instantiateTo(int value, ICause cause) throws ContradictionException {
      return delegate.instantiateTo(value, cause);
   }

   @Override
   public boolean updateLowerBound(int value, ICause cause) throws ContradictionException {
      return delegate.updateLowerBound(value, cause);
   }

   @Override
   public boolean updateUpperBound(int value, ICause cause) throws ContradictionException {
      return delegate.updateUpperBound(value, cause);
   }

   @Override
   public boolean updateBounds(int lb, int ub, ICause cause) throws ContradictionException {
      return delegate.updateBounds(lb, ub, cause);
   }

   @Override
   public boolean contains(int value) {
      return delegate.contains(value);
   }

   @Override
   public boolean isInstantiatedTo(int value) {
      return delegate.isInstantiatedTo(value);
   }

   @Override
   public int getValue() {
      return delegate.getValue();
   }

   @Override
   public int getLB() {
      return delegate.getLB();
   }

   @Override
   public int getUB() {
      return delegate.getUB();
   }

   @Override
   public int getDomainSize() {
      return delegate.getDomainSize();
   }

   @Override
   public int getRange() {
      return delegate.getRange();
   }

   @Override
   public int nextValue(int v) {
      return delegate.nextValue(v);
   }

   @Override
   public int nextValueOut(int v) {
      return delegate.nextValueOut(v);
   }

   @Override
   public int previousValue(int v) {
      return delegate.previousValue(v);
   }

   @Override
   public int previousValueOut(int v) {
      return delegate.previousValueOut(v);
   }

   @Override
   public DisposableValueIterator getValueIterator(boolean bottomUp) {
      return delegate.getValueIterator(bottomUp);
   }

   @Override
   public DisposableRangeIterator getRangeIterator(boolean bottomUp) {
      return delegate.getRangeIterator(bottomUp);
   }

   @Override
   public boolean hasEnumeratedDomain() {
      return delegate.hasEnumeratedDomain();
   }

   @Override
   public IIntDeltaMonitor monitorDelta(ICause propagator) {
      return delegate.monitorDelta(propagator);
   }

   @Override
   public boolean isBool() {
      return delegate.isBool();
   }

   @NotNull
   @Override
   public Iterator<Integer> iterator() {
      return delegate.iterator();
   }

   @Override
   public boolean isInstantiated() {
      return delegate.isInstantiated();
   }

   @Override
   public String getName() {
      return delegate.getName();
   }

   @Override
   public Propagator[] getPropagators() {
      return delegate.getPropagators();
   }

   @Override
   public Propagator getPropagator(int idx) {
      return delegate.getPropagator(idx);
   }

   @Override
   public int getNbProps() {
      return delegate.getNbProps();
   }

   @Override
   public int[] getPIndices() {
      return delegate.getPIndices();
   }

   @Override
   public void setPIndice(int pos, int val) {
      delegate.setPIndice(pos, val);
   }

   @Override
   public int getDindex(int i) {
      return delegate.getDindex(i);
   }

   @Override
   public int getIndexInPropagator(int pidx) {
      return delegate.getIndexInPropagator(pidx);
   }

   @Override
   public void addMonitor(IVariableMonitor monitor) {
      delegate.addMonitor(monitor);
   }

   @Override
   public void removeMonitor(IVariableMonitor monitor) {
      delegate.removeMonitor(monitor);
   }

   @Override
   public void subscribeView(IView view) {
      delegate.subscribeView(view);
   }

   @Override
   public IDelta getDelta() {
      return delegate.getDelta();
   }

   @Override
   public void createDelta() {
      delegate.createDelta();
   }

   @Override
   public int link(Propagator propagator, int idxInProp) {
      return delegate.link(propagator, idxInProp);
   }

   @Override
   public int swapOnPassivate(Propagator propagator, int idxInProp) {
      return delegate.swapOnPassivate(propagator, idxInProp);
   }

   @Override
   public int swapOnActivate(Propagator propagator, int idxInProp) {
      return delegate.swapOnActivate(propagator, idxInProp);
   }

   @Override
   public void unlink(Propagator propagator, int idxInProp) {
      delegate.unlink(propagator, idxInProp);
   }

   @Override
   public void notifyPropagators(IEventType event, ICause cause) throws ContradictionException {
      delegate.notifyPropagators(event, cause);
   }

   @Override
   public void notifyViews(IEventType event, ICause cause) throws ContradictionException {
      delegate.notifyViews(event, cause);
   }

   @Override
   public int getNbViews() {
      return delegate.getNbViews();
   }

   @Override
   public IView getView(int p) {
      return delegate.getView(p);
   }

   @Override
   public void notifyMonitors(IEventType event) throws ContradictionException {
      delegate.notifyMonitors(event);
   }

   @Override
   public void contradiction(ICause cause, String message) throws ContradictionException {
      delegate.contradiction(cause, message);
   }

   @Override
   public Model getModel() {
      return delegate.getModel();
   }

   @Override
   public int getTypeAndKind() {
      return delegate.getTypeAndKind();
   }

   @Override
   public boolean isAConstant() {
      return delegate.isAConstant();
   }

   @Override
   public EvtScheduler getEvtScheduler() {
      return delegate.getEvtScheduler();
   }

   @Override
   public IntVar asIntVar() {
      return delegate.asIntVar();
   }

   @Override
   public BoolVar asBoolVar() {
      return delegate.asBoolVar();
   }

   @Override
   public RealVar asRealVar() {
      return delegate.asRealVar();
   }

   @Override
   public SetVar asSetVar() {
      return delegate.asSetVar();
   }

   @Override
   public void storeEvents(int mask, ICause cause) {
      delegate.storeEvents(mask, cause);
   }

   @Override
   public void clearEvents() {
      delegate.clearEvents();
   }

   @Override
   public int getMask() {
      return delegate.getMask();
   }

   @Override
   public ICause getCause() {
      return delegate.getCause();
   }

   @Override
   public int compareTo(@NotNull Variable variable) {
      return delegate.compareTo(variable);
   }

   @Override
   public int getId() {
      return delegate.getId();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof BoolVarView)) return false;
      BoolVarView integers = (BoolVarView) o;
      return Objects.equals(delegate, integers.delegate);
   }

   @Override
   public int hashCode() {
      return Objects.hash(delegate);
   }
}
