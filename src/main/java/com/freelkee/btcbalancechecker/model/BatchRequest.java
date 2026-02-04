package com.freelkee.btcbalancechecker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequest {
    private String currency;
    private int offset;
    private List<String> addresses;
}
