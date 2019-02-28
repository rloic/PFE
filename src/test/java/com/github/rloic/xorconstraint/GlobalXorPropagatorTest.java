package com.github.rloic.xorconstraint;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.learn.ExplanationForSignedClause;
import org.chocosolver.solver.learn.Implications;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.ValueSortedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GlobalXorPropagatorTest {

    @Test
    void should_return_truth_table_of_custom_xor() {
        Model m = new Model();
        BoolVar[] variables = m.boolVarArray(3);

        BoolVar[][] equations = new BoolVar[][]{
                new BoolVar[]{variables[0], variables[1], variables[2]}
        };

        m.post(new Constraint("GlobalXor", new GlobalXorPropagatorChoco(variables, equations)));
        Solver solver = m.getSolver();

        List<String> result = new ArrayList<>();
        while (solver.solve()) {
            result.add(toString(variables));
        }

        List<String> expected = new ArrayList<>();
        expected.add("[0, 0, 0]");
        expected.add("[0, 1, 1]");
        expected.add("[1, 0, 1]");
        expected.add("[1, 1, 0]");
        expected.add("[1, 1, 1]");

        expected.sort(String::compareTo);
        result.sort(String::compareTo);
        assertEquals(expected, result);
    }

    private String toString(BoolVar[] variables) {
        return Arrays.stream(variables)
                .map(IntVar::getValue)
                .collect(Collectors.toList())
                .toString();
    }

}