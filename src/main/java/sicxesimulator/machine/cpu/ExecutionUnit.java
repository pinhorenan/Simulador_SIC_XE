package sicxesimulator.machine.cpu;

import sicxesimulator.machine.Memory;
import sicxesimulator.models.Instruction;
import sicxesimulator.utils.Convert;

import java.util.logging.Logger;

@SuppressWarnings("unused")
public class ExecutionUnit {

    private static final Logger logger = Logger.getLogger(ExecutionUnit.class.getName());
    private final RegisterSet registers;
    private final Memory memory;

    public ExecutionUnit(RegisterSet registers, Memory memory) {
        this.registers = registers;
        this.memory = memory;
    }

    /**
     * Converte o endereço efetivo (em bytes) para o endereço de palavra (cada palavra tem 3 bytes).
     * Lança exceção se o endereço não estiver alinhado.
     */
    private int toWordAddress(int effectiveAddress) {
        if (effectiveAddress % 3 != 0) {
            throw new IllegalArgumentException("Endereço efetivo não alinhado: " + effectiveAddress);
        }
        return effectiveAddress / 3;
    }

    // ===============================================================
    // Métodos para operações aritméticas e lógicas (inteiras)
    // ===============================================================

    public String executeADD(int[] operands, boolean indexed, int effectiveAddress) {
        logger.fine(String.format("executeADD: effectiveAddress = %06X", effectiveAddress));
        Register A = registers.getRegister("A");
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int operandValue = Convert.bytesToInt(wordBytes);
        logger.fine(String.format("executeADD: Valor lido da memória = %06X", operandValue));

        int result = A.getIntValue() + operandValue;
        A.setValue(result);
        updateConditionCode(result);
        String log = String.format("ADD: Resultado = %06X", result);
        logger.info(log);
        return log;
    }

    public String executeADDR(int[] operands) {
        // operands[0] = r1, operands[1] = r2
        Register reg1 = getRegisterByNumber(operands[0]);
        Register reg2 = getRegisterByNumber(operands[1]);
        int result = reg1.getIntValue() + reg2.getIntValue();
        reg2.setValue(result);
        updateConditionCode(result);
        String log = String.format("ADDR: R%d + R%d = %06X", operands[0], operands[1], result);
        logger.info(log);
        return log;
    }

    public String executeAND(int[] operands, boolean indexed, int effectiveAddress) {
        logger.fine(String.format("executeAND: effectiveAddress = %06X", effectiveAddress));
        Register A = registers.getRegister("A");
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int operandValue = Convert.bytesToInt(wordBytes);
        logger.fine(String.format("executeAND: Valor lido da memória = %06X", operandValue));

        int result = A.getIntValue() & operandValue;
        A.setValue(result);
        updateConditionCode(result);
        String log = String.format("AND: Resultado = %06X", result);
        logger.info(log);
        return log;
    }

    public String executeSUB(int[] operands, boolean indexed, int effectiveAddress) {
        Register A = registers.getRegister("A");
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int operandValue = Convert.bytesToInt(wordBytes);
        int result = A.getIntValue() - operandValue;
        A.setValue(result);
        updateConditionCode(result);
        String log = String.format("SUB: Resultado = %06X", result);
        logger.info(log);
        return log;
    }

    public String executeSUBR(int[] operands) {
        Register r1 = getRegisterByNumber(operands[0]);
        Register r2 = getRegisterByNumber(operands[1]);
        int result = r2.getIntValue() - r1.getIntValue();
        r2.setValue(result);
        updateConditionCode(result);
        String log = String.format("SUBR: R%d - R%d = %06X", operands[1], operands[0], result);
        logger.info(log);
        return log;
    }

    public String executeDIV(int[] operands, boolean indexed, int effectiveAddress) {
        Register A = registers.getRegister("A");
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int divisor = Convert.bytesToInt(wordBytes);
        if (divisor == 0) {
            throw new ArithmeticException("Divisão por zero");
        }
        int result = A.getIntValue() / divisor;
        A.setValue(result);
        updateConditionCode(result);
        String log = String.format("DIV: Resultado = %06X", result);
        logger.info(log);
        return log;
    }

    public String executeDIVR(int[] operands) {
        Register r1 = getRegisterByNumber(operands[0]);
        Register r2 = getRegisterByNumber(operands[1]);
        if (r1.getIntValue() == 0) {
            throw new ArithmeticException("Divisão por zero");
        }
        int result = r2.getIntValue() / r1.getIntValue();
        r2.setValue(result);
        updateConditionCode(result);
        String log = String.format("DIVR: R%d / R%d = %06X", operands[1], operands[0], result);
        logger.info(log);
        return log;
    }

    public String executeMUL(int[] operands, boolean indexed, int effectiveAddress) {
        Register A = registers.getRegister("A");
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int operandValue = Convert.bytesToInt(wordBytes);
        int result = A.getIntValue() * operandValue;
        A.setValue(result);
        updateConditionCode(result);
        String log = String.format("MUL: Resultado = %06X", result);
        logger.info(log);
        return log;
    }

    public String executeSUBF(int[] operands, boolean indexed, int effectiveAddress) {
        Register F = registers.getRegister("F");
        int wordAddr = toWordAddress(effectiveAddress);
        byte[] firstWord = memory.readWord(wordAddr);
        byte[] secondWord = memory.readWord(wordAddr + 1);
        byte[] fBytes = new byte[6];
        System.arraycopy(firstWord, 0, fBytes, 0, 3);
        System.arraycopy(secondWord, 0, fBytes, 3, 3);
        long operandF = bytesToLong48(fBytes);
        long currentF = F.getLongValue();
        long result = currentF - operandF;
        F.setValue(result);
        String log = String.format("SUBF: Resultado = %012X", result);
        logger.info(log);
        return log;
    }

    public String executeMULR(int[] operands) {
        Register r1 = getRegisterByNumber(operands[0]);
        Register r2 = getRegisterByNumber(operands[1]);
        int result = r1.getIntValue() * r2.getIntValue();
        r2.setValue(result);
        updateConditionCode(result);
        String log = String.format("MULR: R%d * R%d = %06X", operands[0], operands[1], result);
        logger.info(log);
        return log;
    }

    // ===============================================================
    // Operações de ponto flutuante (48 bits, para o registrador F)
    // ===============================================================

    public String executeADDF(int[] operands, boolean indexed, int effectiveAddress) {
        Register F = registers.getRegister("F");
        int wordAddr = toWordAddress(effectiveAddress);
        byte[] firstWord = memory.readWord(wordAddr);
        byte[] secondWord = memory.readWord(wordAddr + 1);
        byte[] fBytes = new byte[6];
        System.arraycopy(firstWord, 0, fBytes, 0, 3);
        System.arraycopy(secondWord, 0, fBytes, 3, 3);
        long operandF = bytesToLong48(fBytes);
        long currentF = F.getLongValue();
        long result = currentF + operandF;
        F.setValue(result);
        String log = String.format("ADDF: Resultado = %012X", result);
        logger.info(log);
        return log;
    }

    public String executeCOMPF(int[] operands, boolean indexed, int effectiveAddress) {
        Register F = registers.getRegister("F");
        int wordAddr = toWordAddress(effectiveAddress);
        byte[] firstWord = memory.readWord(wordAddr);
        byte[] secondWord = memory.readWord(wordAddr + 1);
        byte[] fBytes = new byte[6];
        System.arraycopy(firstWord, 0, fBytes, 0, 3);
        System.arraycopy(secondWord, 0, fBytes, 3, 3);
        long memF = bytesToLong48(fBytes);
        long currentF = F.getLongValue();
        long diff = currentF - memF;
        updateConditionCode((int) diff); // Aproximação para condição
        String log = String.format("COMPF: F=%012X vs Mem[%06X]=%012X => %s",
                currentF, effectiveAddress, memF, getConditionCodeDescription());
        logger.info(log);
        return log;
    }

    public String executeDIVF(int[] operands, boolean indexed, int effectiveAddress) {
        Register F = registers.getRegister("F");
        int wordAddr = toWordAddress(effectiveAddress);
        byte[] firstWord = memory.readWord(wordAddr);
        byte[] secondWord = memory.readWord(wordAddr + 1);
        byte[] fBytes = new byte[6];
        System.arraycopy(firstWord, 0, fBytes, 0, 3);
        System.arraycopy(secondWord, 0, fBytes, 3, 3);
        long divisor = bytesToLong48(fBytes);
        if (divisor == 0) {
            throw new ArithmeticException("Divisão por zero em DIVF");
        }
        long currentF = F.getLongValue();
        long result = currentF / divisor;
        F.setValue(result);
        String log = String.format("DIVF: Resultado = %012X", result);
        logger.info(log);
        return log;
    }

    public String executeMULF(int[] operands, boolean indexed, int effectiveAddress) {
        Register F = registers.getRegister("F");
        int wordAddr = toWordAddress(effectiveAddress);
        byte[] firstWord = memory.readWord(wordAddr);
        byte[] secondWord = memory.readWord(wordAddr + 1);
        byte[] fBytes = new byte[6];
        System.arraycopy(firstWord, 0, fBytes, 0, 3);
        System.arraycopy(secondWord, 0, fBytes, 3, 3);
        long operandF = bytesToLong48(fBytes);
        long currentF = F.getLongValue();
        long result = currentF * operandF;
        F.setValue(result);
        String log = String.format("MULF: Resultado = %012X", result);
        logger.info(log);
        return log;
    }

    public String executeFIX() {
        // Converte o valor de F para inteiro e armazena em A (truncamento simples)
        Register F = registers.getRegister("F");
        Register A = registers.getRegister("A");
        int fixedValue = (int) F.getLongValue();
        A.setValue(fixedValue);
        String log = String.format("FIX: A ← %06X", fixedValue);
        logger.info(log);
        return log;
    }

    public String executeFLOAT() {
        // Converte o valor inteiro de A para ponto flutuante e armazena em F
        Register A = registers.getRegister("A");
        Register F = registers.getRegister("F");
        long floatValue = A.getIntValue(); // Conversão simples
        F.setValue(floatValue);
        String log = String.format("FLOAT: F ← %012X", floatValue);
        logger.info(log);
        return log;
    }

    public String executeNORM() {
        String log = "NORM: Operação de normalização não implementada.";
        logger.info(log);
        return log;
    }

    // ===============================================================
    // Operações de controle de fluxo (saltos e sub-rotinas)
    // ===============================================================

    public String executeJ(int[] operands, boolean indexed, int effectiveAddress) {
        registers.getRegister("PC").setValue(effectiveAddress);
        String log = String.format("J: PC ← %06X", effectiveAddress);
        logger.info(log);
        return log;
    }

    public String executeJEQ(int[] operands, boolean indexed, int effectiveAddress) {
        if (registers.getRegister("SW").getIntValue() == 0) {
            registers.getRegister("PC").setValue(effectiveAddress);
            String log = String.format("JEQ: PC ← %06X", effectiveAddress);
            logger.info(log);
            return log;
        } else {
            String log = "JEQ: Condição não satisfeita";
            logger.info(log);
            return log;
        }
    }

    public String executeJGT(int[] operands, boolean indexed, int effectiveAddress) {
        if (registers.getRegister("SW").getIntValue() == 2) {
            registers.getRegister("PC").setValue(effectiveAddress);
            String log = String.format("JGT: PC ← %06X", effectiveAddress);
            logger.info(log);
            return log;
        } else {
            String log = "JGT: Condição não satisfeita";
            logger.info(log);
            return log;
        }
    }

    public String executeJLT(int[] operands, boolean indexed, int effectiveAddress) {
        if (registers.getRegister("SW").getIntValue() == 1) {
            registers.getRegister("PC").setValue(effectiveAddress);
            String log = String.format("JLT: PC ← %06X", effectiveAddress);
            logger.info(log);
            return log;
        } else {
            String log = "JLT: Condição não satisfeita";
            logger.info(log);
            return log;
        }
    }

    public String executeJSUB(int[] operands, boolean indexed, int effectiveAddress) {
        int returnAddress = registers.getRegister("PC").getIntValue();
        registers.getRegister("L").setValue(returnAddress);
        registers.getRegister("PC").setValue(effectiveAddress);
        String log = String.format("JSUB: PC ← %06X | L = %06X", effectiveAddress, returnAddress);
        logger.info(log);
        return log;
    }

    public String executeRSUB() {
        int returnAddress = registers.getRegister("L").getIntValue();
        if (returnAddress == 0) {
            registers.getRegister("PC").setValue(0);
            String log = "RSUB: Encerrando execução (HALT).";
            logger.info(log);
            return log;
        }
        registers.getRegister("PC").setValue(returnAddress);
        String log = String.format("RSUB: Retornando para %06X", returnAddress);
        logger.info(log);
        return log;
    }

    // ===============================================================
    // Operações de carregamento (load) e armazenamento (store)
    // ===============================================================

    public String executeLDA(int[] operands, boolean indexed, int effectiveAddress) {
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int value = Convert.bytesToInt(wordBytes);
        registers.getRegister("A").setValue(value);
        String log = String.format("LDA: A ← %06X", value);
        logger.info(log);
        return log;
    }

    public String executeLDB(int[] operands, boolean indexed, int effectiveAddress) {
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int value = Convert.bytesToInt(wordBytes);
        registers.getRegister("B").setValue(value);
        String log = String.format("LDB: B ← %06X", value);
        logger.info(log);
        return log;
    }

    public String executeLDCH(int[] operands, boolean indexed, int effectiveAddress) {
        int byteValue = memory.readByte(toWordAddress(effectiveAddress));
        Register A = registers.getRegister("A");
        int currentA = A.getIntValue();
        int newA = (currentA & 0xFFFF00) | (byteValue & 0xFF);
        A.setValue(newA);
        String log = String.format("LDCH: A[byte] ← %02X", byteValue);
        logger.info(log);
        return log;
    }

    public String executeLDF(int[] operands, boolean indexed, int effectiveAddress) {
        int wordAddr = toWordAddress(effectiveAddress);
        byte[] firstWord = memory.readWord(wordAddr);
        byte[] secondWord = memory.readWord(wordAddr + 1);
        byte[] fBytes = new byte[6];
        System.arraycopy(firstWord, 0, fBytes, 0, 3);
        System.arraycopy(secondWord, 0, fBytes, 3, 3);
        long value = bytesToLong48(fBytes);
        registers.getRegister("F").setValue(value);
        String log = String.format("LDF: F ← %012X", value);
        logger.info(log);
        return log;
    }

    public String executeLDL(int[] operands, boolean indexed, int effectiveAddress) {
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int value = Convert.bytesToInt(wordBytes);
        registers.getRegister("L").setValue(value);
        String log = String.format("LDL: L ← %06X", value);
        logger.info(log);
        return log;
    }

    public String executeLDS(int[] operands, boolean indexed, int effectiveAddress) {
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int value = Convert.bytesToInt(wordBytes);
        registers.getRegister("S").setValue(value);
        String log = String.format("LDS: S ← %06X", value);
        logger.info(log);
        return log;
    }

    /**
     * Executa a operação CLEAR ou LDX.
     * Se houver apenas um operando, assume CLEAR (zera o registrador indicado).
     * Caso contrário, trata como LDX (carrega o valor da memória no registrador X).
     */
    public String executeCLEAR_LDX(Instruction instruction, int[] operands) {
        if (operands.length == 1) { // CLEAR
            Register reg = getRegisterByNumber(operands[0]);
            reg.setValue(0);
            String log = String.format("CLEAR: R%d zerado", operands[0]);
            logger.info(log);
            return log;
        } else { // LDX
            int effectiveAddress = instruction.effectiveAddress();
            byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
            int value = Convert.bytesToInt(wordBytes);
            registers.getRegister("X").setValue(value);
            String log = String.format("LDX: Carregado %06X", value);
            logger.info(log);
            return log;
        }
    }

    /**
     * Executa a operação COMP: compara o acumulador com o valor lido da memória.
     */
    public String executeCOMP(int[] operands, boolean indexed, int effectiveAddress) {
        Register A = registers.getRegister("A");
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int memValue = Convert.bytesToInt(wordBytes);
        int comparison = A.getIntValue() - memValue;
        updateConditionCode(comparison);
        String log = String.format("COMP: A=%06X vs Mem[%06X]=%06X => %s",
                A.getIntValue(), effectiveAddress, memValue, getConditionCodeDescription());
        logger.info(log);
        return log;
    }

    /**
     * Executa a operação COMPR: compara dois registradores.
     */
    public String executeCOMPR(int[] operands) {
        Register r1 = getRegisterByNumber(operands[0]);
        Register r2 = getRegisterByNumber(operands[1]);
        int comparison = r1.getIntValue() - r2.getIntValue();
        updateConditionCode(comparison);
        String log = String.format("COMPR: R%d=%06X vs R%d=%06X => %s",
                operands[0], r1.getIntValue(), operands[1], r2.getIntValue(), getConditionCodeDescription());
        logger.info(log);
        return log;
    }

    /**
     * Executa a operação OR: realiza uma operação OR bit a bit entre o acumulador e o valor da memória.
     */
    public String executeOR(int[] operands, boolean indexed, int effectiveAddress) {
        Register A = registers.getRegister("A");
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int operandValue = Convert.bytesToInt(wordBytes);
        int result = A.getIntValue() | operandValue;
        A.setValue(result);
        updateConditionCode(result);
        String log = String.format("OR: Resultado = %06X", result);
        logger.info(log);
        return log;
    }

    /**
     * Executa o deslocamento para a esquerda (SHIFTL).
     * O primeiro operando indica o registrador, e o segundo a quantidade de bits.
     */
    public String executeSHIFTL(int[] operands) {
        Register reg = getRegisterByNumber(operands[0]);
        int count = operands[1];
        int value = reg.getIntValue() << count;
        reg.setValue(value);
        updateConditionCode(value);
        String log = String.format("SHIFTL: R%d << %d = %06X", operands[0], count, value);
        logger.info(log);
        return log;
    }

    /**
     * Executa o deslocamento lógico para a direita (SHIFTR).
     * O primeiro operando indica o registrador, e o segundo a quantidade de bits.
     */
    public String executeSHIFTR(int[] operands) {
        Register reg = getRegisterByNumber(operands[0]);
        int count = operands[1];
        int value = reg.getIntValue() >>> count; // Deslocamento lógico
        reg.setValue(value);
        updateConditionCode(value);
        String log = String.format("SHIFTR: R%d >> %d = %06X", operands[0], count, value);
        logger.info(log);
        return log;
    }


    public String executeLDT(int[] operands, boolean indexed, int effectiveAddress) {
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int value = Convert.bytesToInt(wordBytes);
        registers.getRegister("T").setValue(value);
        String log = String.format("LDT: T ← %06X", value);
        logger.info(log);
        return log;
    }

    public String executeLDX(int[] operands, boolean indexed, int effectiveAddress) {
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int value = Convert.bytesToInt(wordBytes);
        registers.getRegister("X").setValue(value);
        String log = String.format("LDX: X ← %06X", value);
        logger.info(log);
        return log;
    }

    public String executeLPS(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "LPS: Operação não implementada.";
        logger.info(log);
        return log;
    }

    public String executeSTA(int[] operands, boolean indexed, int effectiveAddress) {
        int value = registers.getRegister("A").getIntValue();
        memory.writeWord(toWordAddress(effectiveAddress), Convert.intTo3Bytes(value));
        String log = String.format("STA: Mem[%06X] ← %06X", effectiveAddress, value);
        logger.info(log);
        return log;
    }

    public String executeSTB(int[] operands, boolean indexed, int effectiveAddress) {
        int value = registers.getRegister("B").getIntValue();
        memory.writeWord(toWordAddress(effectiveAddress), Convert.intTo3Bytes(value));
        String log = String.format("STB: Mem[%06X] ← %06X", effectiveAddress, value);
        logger.info(log);
        return log;
    }

    public String executeSTCH(int[] operands, boolean indexed, int effectiveAddress) {
        int byteValue = registers.getRegister("A").getIntValue() & 0xFF;
        memory.writeByte(toWordAddress(effectiveAddress),  byteValue);
        String log = String.format("STCH: Mem[%06X] ← %02X", effectiveAddress, byteValue);
        logger.info(log);
        return log;
    }

    public String executeSTF(int[] operands, boolean indexed, int effectiveAddress) {
        Register F = registers.getRegister("F");
        long fValue = F.getLongValue();
        byte[] bytes = long48ToBytes(fValue);
        int wordAddr = toWordAddress(effectiveAddress);
        byte[] firstWord = new byte[3];
        byte[] secondWord = new byte[3];
        System.arraycopy(bytes, 0, firstWord, 0, 3);
        System.arraycopy(bytes, 3, secondWord, 0, 3);
        memory.writeWord(wordAddr, firstWord);
        memory.writeWord(wordAddr + 1, secondWord);
        String log = String.format("STF: Mem[%06X] ← %012X", effectiveAddress, fValue);
        logger.info(log);
        return log;
    }

    public String executeSTI(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "STI: Operação de armazenamento para dispositivo não implementada.";
        logger.info(log);
        return log;
    }

    public String executeSTL(int[] operands, boolean indexed, int effectiveAddress) {
        int value = registers.getRegister("L").getIntValue();
        memory.writeWord(toWordAddress(effectiveAddress), Convert.intTo3Bytes(value));
        String log = String.format("STL: Mem[%06X] ← %06X", effectiveAddress, value);
        logger.info(log);
        return log;
    }

    public String executeSTS(int[] operands, boolean indexed, int effectiveAddress) {
        int value = registers.getRegister("S").getIntValue();
        memory.writeWord(toWordAddress(effectiveAddress), Convert.intTo3Bytes(value));
        String log = String.format("STS: Mem[%06X] ← %06X", effectiveAddress, value);
        logger.info(log);
        return log;
    }

    public String executeSTSW(int[] operands, boolean indexed, int effectiveAddress) {
        int value = registers.getRegister("SW").getIntValue();
        memory.writeWord(toWordAddress(effectiveAddress), Convert.intTo3Bytes(value));
        String log = String.format("STSW: Mem[%06X] ← %06X", effectiveAddress, value);
        logger.info(log);
        return log;
    }

    public String executeSTT(int[] operands, boolean indexed, int effectiveAddress) {
        int value = registers.getRegister("T").getIntValue();
        memory.writeWord(toWordAddress(effectiveAddress), Convert.intTo3Bytes(value));
        String log = String.format("STT: Mem[%06X] ← %06X", effectiveAddress, value);
        logger.info(log);
        return log;
    }

    public String executeSTX(int[] operands, boolean indexed, int effectiveAddress) {
        int value = registers.getRegister("X").getIntValue();
        memory.writeWord(toWordAddress(effectiveAddress), Convert.intTo3Bytes(value));
        String log = String.format("STX: Mem[%06X] ← %06X", effectiveAddress, value);
        logger.info(log);
        return log;
    }

    // ===============================================================
    // Operações de I/O e controle especial
    // ===============================================================

    public String executeSIO(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "SIO: Início de I/O não implementado.";
        logger.info(log);
        return log;
    }

    public String executeSSK(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "SSK: Operação de proteção não implementada.";
        logger.info(log);
        return log;
    }

    public String executeRD(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "RD: Operação de leitura de dispositivo não implementada.";
        logger.info(log);
        return log;
    }

    public String executeRMO(int[] operands) {
        Register source = getRegisterByNumber(operands[0]);
        Register dest = getRegisterByNumber(operands[1]);
        dest.setValue(source.getIntValue());
        String log = String.format("RMO: R%d → R%d | Valor = %06X", operands[0], operands[1], source.getIntValue());
        logger.info(log);
        return log;
    }

    public String executeTIO(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "TIO: Teste de I/O não implementado.";
        logger.info(log);
        return log;
    }

    public String executeTD(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "TD: Teste de dispositivo não implementado.";
        logger.info(log);
        return log;
    }

    public String executeTIX(int[] operands, boolean indexed, int effectiveAddress) {
        Register X = registers.getRegister("X");
        X.setValue(X.getIntValue() + 1);
        byte[] wordBytes = memory.readWord(toWordAddress(effectiveAddress));
        int memValue = Convert.bytesToInt(wordBytes);
        int comparison = X.getIntValue() - memValue;
        updateConditionCode(comparison);
        String log = String.format("TIX: X=%06X vs Mem[%06X]=%06X => %s",
                X.getIntValue(), effectiveAddress, memValue, getConditionCodeDescription());
        logger.info(log);
        return log;
    }

    public String executeTIXR(int[] operands) {
        Register X = registers.getRegister("X");
        X.setValue(X.getIntValue() + 1);
        Register r = getRegisterByNumber(operands[0]);
        int comparison = X.getIntValue() - r.getIntValue();
        updateConditionCode(comparison);
        String log = String.format("TIXR: X=%06X vs R%d=%06X => %s",
                X.getIntValue(), operands[0], r.getIntValue(), getConditionCodeDescription());
        logger.info(log);
        return log;
    }

    public String executeSVC(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "SVC: Chamada de sistema não implementada.";
        logger.info(log);
        return log;
    }

    public String executeWD(int[] operands, boolean indexed, int effectiveAddress) {
        String log = "WD: Escrita para dispositivo não implementada.";
        logger.info(log);
        return log;
    }

    // ===============================================================
    // Métodos auxiliares
    // ===============================================================

    /**
     * Atualiza a palavra de condição (SW) com base no valor calculado:
     *  0 para igual, 1 para menor, 2 para maior.
     */
    private void updateConditionCode(int value) {
        int cc = (value == 0) ? 0 : (value < 0 ? 1 : 2);
        registers.getRegister("SW").setValue(cc);
    }

    /**
     * Retorna a descrição textual do código condicional (SW).
     */
    private String getConditionCodeDescription() {
        return switch (registers.getRegister("SW").getIntValue()) {
            case 0 -> "Igual";
            case 1 -> "Menor";
            case 2 -> "Maior";
            default -> "Desconhecido";
        };
    }

    /**
     * Retorna o registrador conforme o número:
     * 0=A, 1=X, 2=L, 3=B, 4=S, 5=T.
     */
    private Register getRegisterByNumber(int num) {
        return switch (num) {
            case 0 -> registers.getRegister("A");
            case 1 -> registers.getRegister("X");
            case 2 -> registers.getRegister("L");
            case 3 -> registers.getRegister("B");
            case 4 -> registers.getRegister("S");
            case 5 -> registers.getRegister("T");
            default -> throw new IllegalArgumentException("Registrador inválido: " + num);
        };
    }

    /**
     * Converte um array de 6 bytes em um valor long (48 bits).
     */
    private long bytesToLong48(byte[] bytes) {
        if (bytes.length != 6) {
            throw new IllegalArgumentException("Array deve ter 6 bytes para valor de 48 bits.");
        }
        return ((long)(bytes[0] & 0xFF) << 40) |
                ((long)(bytes[1] & 0xFF) << 32) |
                ((long)(bytes[2] & 0xFF) << 24) |
                ((long)(bytes[3] & 0xFF) << 16) |
                ((long)(bytes[4] & 0xFF) << 8)  |
                ((long)(bytes[5] & 0xFF));
    }

    /**
     * Converte um valor long (48 bits) em um array de 6 bytes.
     */
    private byte[] long48ToBytes(long value) {
        byte[] bytes = new byte[6];
        bytes[0] = (byte)((value >> 40) & 0xFF);
        bytes[1] = (byte)((value >> 32) & 0xFF);
        bytes[2] = (byte)((value >> 24) & 0xFF);
        bytes[3] = (byte)((value >> 16) & 0xFF);
        bytes[4] = (byte)((value >> 8) & 0xFF);
        bytes[5] = (byte)(value & 0xFF);
        return bytes;
    }
}
