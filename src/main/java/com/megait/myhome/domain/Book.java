package com.megait.myhome.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@Getter @Setter
@DiscriminatorValue("B")
@NoArgsConstructor
public class Book extends Item {

    private String isbn;
}
