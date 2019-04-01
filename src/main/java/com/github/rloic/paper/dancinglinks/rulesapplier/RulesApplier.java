package com.github.rloic.paper.dancinglinks.rulesapplier;

import com.github.rloic.paper.dancinglinks.actions.IUpdater;

public interface RulesApplier {

   IUpdater buildTrueAssignation(int variable);

   IUpdater buildFalseAssignation(int variable);
}
