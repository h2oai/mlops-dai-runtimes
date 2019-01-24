package ai.h2o.mojos.deploy.kdb;

import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import ai.h2o.mojos.runtime.frame.MojoColumn.Type;
import ai.h2o.mojos.runtime.lic.LicenseException;
import kx.c;
import java.io.IOException;


class MojoKdbTransformTest {

    @Test
    void validateMojoTransform() throws IOException {
        // Given
        String dropCols = "";
        MojoFrameMeta mojoMetaInput = generateMojoFrameMetaInput();
        c.Flip kdbFlipTable = generateDummyFlip();

        // When
        MojoFrame iframe = MojoKdbTransform.createMojoFrameFromKdbFlip(mojoMetaInput, kdbFlipTable, dropCols);
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
        assertThat(kdbPublishObject.length == 3);
        assertThat(kdbPublishObject[0].equals(".u.upd"));
        assertThat(kdbPublishObject[1].equals("fooTable"));
        assertThat(kdbPublishObject[2] instanceof Object[]);
    }

    private c.Flip generateDummyFlip() {
        int[] limBal = {20000, 120000};
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

    private MojoFrameMeta generateMojoFrameMetaInput() {
        Type[] types = {Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64,
                Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64,
                Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64, Type.Int64};
        String[] names = {"LIMIT_BAL", "SEX", "EDUCATION", "MARRIAGE", "AGE", "PAY_1",
                "PAY_2", "PAY_3", "PAY_4", "PAY_5", "PAY_6", "BILL_AMT1", "BILL_AMT2", "BILL_AMT3", "BILL_AMT4",
                "BILL_AMT5", "BILL_AMT6", "PAY_AMT1", "PAY_AMT2", "PAY_AMT3", "PAY_AMT4", "PAY_AMT5", "PAY_AMT6"};
        return new MojoFrameMeta(names, types);
    }

    private MojoFrame generateDummyTransformedMojoFrame() {
        Type[] types = {Type.Float64, Type.Float64};
        String[] names = {"Prediction.0", "Prediction.1"};
        MojoFrameMeta meta = new MojoFrameMeta(names, types);
        MojoFrameBuilder frameBuilder = new MojoFrameBuilder(meta);
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
