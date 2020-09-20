package ai.h2o.mojos.deploy.kdb;

import ai.h2o.mojos.runtime.api.MojoColumnMeta;
import ai.h2o.mojos.runtime.frame.MojoColumn.Type;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kx.c;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.singletonList;


class MojoKdbTransformTest {

    @Test
    void validateMojoTransform() throws IOException {
        // Given
        String dropCols = "";
        MojoFrameBuilder frameBuilder = generateMojoFrameBuilder();
        c.Flip kdbFlipTable = generateDummyFlip();

        // When
        MojoFrame iframe = MojoKdbTransform.createMojoFrameFromKdbFlip(frameBuilder, kdbFlipTable, dropCols);
        iframe.debug();

        // Then
        assertThat(iframe.getNcols()).isEqualTo(23);
        assertThat(iframe.getNrows()).isEqualTo(2);
    }

    @Test
    void validateKdbPublishObjectGeneration() {
        // Given
        String pubTable = "fooTable";
        MojoFrame oframe = generateDummyTransformedMojoFrame();
        c.Flip kdbFlipTable = generateDummyFlip();
        oframe.debug();

        // When
        Object[] kdbPublishObject = MojoKdbTransform.generateMojoPredictionPublishObject(pubTable, oframe, kdbFlipTable);

        // Then
        assertThat(kdbPublishObject.length).isEqualTo(3);
        assertThat(kdbPublishObject[0].equals(".u.upd"));
        assertThat(kdbPublishObject[1].equals("fooTable"));
        assertThat(kdbPublishObject[2]).isInstanceOf(Object[].class);
    }

    private c.Flip generateDummyFlip() {
        String[] limBal = {"20000", "NA"};
        int[] sex = {1, 2};
        int[] education = {5, 1};
        int[] marriage = {3,0};
        int[] age = {25, 64};
        int[] pay1 = {2, -1};
        int[] pay2 = {2, 2};
        int[] pay3 = {-1, 0};
        int[] pay4 = {-1, 0};
        int[] pay5 = {-2, 0};
        int[] pay6 = {-2, 2};
        int[] bill1 = {3913, 2682};
        int[] bill2 = {3102, 1725};
        int[] bill3 = {689, 2682};
        int[] bill4 = {0, 3272};
        int[] bill5 = {0, 3455};
        int[] bill6 = {0, 3261};
        int[] payamt1 = {0, 0};
        int[] payamt2 = {689, 1000};
        int[] payamt3 = {0, 1000};
        int[] payamt4 = {0, 1000};
        int[] payamt5 = {0, 0};
        int[] payamt6 = {0, 2000};
        Object[] data = new Object[] {limBal, sex, education, marriage, age, pay1, pay2, pay3, pay4, pay5, pay6, bill1, bill2, bill3, bill4, bill5, bill6,
                payamt1, payamt2, payamt3, payamt4, payamt5, payamt6};
        String[] columnNames = new String[] {"LIMIT_BAL", "SEX", "EDUCATION", "MARRIAGE", "AGE", "PAY_1",
                "PAY_2", "PAY_3", "PAY_4", "PAY_5", "PAY_6", "BILL_AMT1", "BILL_AMT2", "BILL_AMT3", "BILL_AMT4",
                "BILL_AMT5", "BILL_AMT6", "PAY_AMT1", "PAY_AMT2", "PAY_AMT3", "PAY_AMT4", "PAY_AMT5", "PAY_AMT6"};
        c.Dict dict = new c.Dict(columnNames, data);
        return new c.Flip(dict);
    }

    private MojoFrameBuilder generateMojoFrameBuilder() {
        String[] names = {"LIMIT_BAL", "SEX", "EDUCATION", "MARRIAGE", "AGE", "PAY_1",
                "PAY_2", "PAY_3", "PAY_4", "PAY_5", "PAY_6", "BILL_AMT1", "BILL_AMT2", "BILL_AMT3", "BILL_AMT4",
                "BILL_AMT5", "BILL_AMT6", "PAY_AMT1", "PAY_AMT2", "PAY_AMT3", "PAY_AMT4", "PAY_AMT5", "PAY_AMT6"};
        final List<MojoColumnMeta> columns = new ArrayList<>();
        for (String name : names) {
            columns.add(MojoColumnMeta.newOutput(name, Type.Int64));
        }
        return new MojoFrameBuilder(new MojoFrameMeta(columns), singletonList("NA"), Collections.emptyMap());
    }

    private MojoFrame generateDummyTransformedMojoFrame() {
        final List<MojoColumnMeta> columns = new ArrayList<>();
        columns.add(MojoColumnMeta.newOutput("Prediction.0", Type.Float64));
        columns.add(MojoColumnMeta.newOutput("Prediction.1", Type.Float64));
        final MojoFrameMeta meta = new MojoFrameMeta(columns);
        final MojoFrameBuilder frameBuilder = new MojoFrameBuilder(meta, Collections.emptyList(), Collections.emptyMap());
        MojoRowBuilder mojoRowBuilder = frameBuilder.getMojoRowBuilder();
        mojoRowBuilder.setValue("Prediction.0", "0.64");
        mojoRowBuilder.setValue("Prediction.1", "0.36");
        frameBuilder.addRow(mojoRowBuilder);
        mojoRowBuilder = frameBuilder.getMojoRowBuilder();
        mojoRowBuilder.setValue("Prediction.0", "0.13");
        mojoRowBuilder.setValue("Prediction.1", "0.87");
        frameBuilder.addRow(mojoRowBuilder);
        return frameBuilder.toMojoFrame();
    }
}
