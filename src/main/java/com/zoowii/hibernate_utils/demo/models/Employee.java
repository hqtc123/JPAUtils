package com.zoowii.hibernate_utils.demo.models;

import com.zoowii.hibernate_utils.orm.Model;
import com.zoowii.hibernate_utils.query.Finder;

import javax.persistence.*;

@Entity
@Table(name = "employee")
public class Employee extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private int age;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public static Finder<Employee, Integer> find = new Finder<Employee, Integer>(Employee.class, Integer.class);
}
