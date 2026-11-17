package com.example.app.results.entity;

import jakarta.persistence.*;

/** One row of the KQKN-DV "Test results" table. Rows are replaced as a set while the dossier is editable. */
@Entity
@Table(name = "result_release_items")
public class ResultReleaseItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "release_id") private ResultRelease release;
    @Column(nullable = false) private int rowOrder;
    @Column(name = "parameter_name", nullable = false, length = 200) private String parameterName;
    @Column(length = 60) private String unit;
    @Column(length = 200) private String method;
    @Column(name = "result_value", nullable = false, length = 120) private String resultValue;
    @Column(length = 255) private String note;

    protected ResultReleaseItem() {}
    public ResultReleaseItem(ResultRelease release, int rowOrder, String parameterName, String unit, String method,
                             String resultValue, String note) {
        this.release = release; this.rowOrder = rowOrder; this.parameterName = parameterName; this.unit = unit;
        this.method = method; this.resultValue = resultValue; this.note = note;
    }

    public Long getId() { return id; }
    public int getRowOrder() { return rowOrder; }
    public String getParameterName() { return parameterName; }
    public String getUnit() { return unit; }
    public String getMethod() { return method; }
    public String getResultValue() { return resultValue; }
    public String getNote() { return note; }
}
