package tigerc.translate.jvm;

public class Label {
    private static int serialNum = 0;
    private String name;

    /**
     * Use this constructor for intraprocedural labels, which must be unique WRT
     * both the method in which they occur and the overall collection of labels
     * generated for this byte code file.
     * 
     * @param baseID
     *            "serial" number of the JVMGeneratorV object from which this Label
     *            is created.
     * @param text
     *            the base text of this Label
     */
    public Label(int baseID, String text) {
        name = text.toUpperCase() + "$" + baseID + "_" + serialNum++;
    }

    /**
     * Use this constructor for Labels whose text must be exactly the argument
     * provided.
     * 
     * @param text
     *            - the text of this Label
     */
    public Label(String text) {
        this.name = text;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Label) && ((Label) o).name.equals(name);
    }
}
