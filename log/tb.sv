// Code your testbench here
// or browse Examples
`include "uvm_macros.svh"
import uvm_pkg::*;

class my_obj extends uvm_object;

    rand int a,b,c;

    constraint c_c { a < 10; b < 10; c < 10; }

  `uvm_object_utils_begin(my_obj)
  `uvm_field_int(a, UVM_DEFAULT)
  `uvm_field_int(b, UVM_DEFAULT)
  `uvm_field_int(c, UVM_DEFAULT)
  `uvm_object_utils_end

endclass

class my_component extends uvm_component;
    rand my_obj o;

  `uvm_component_utils(my_component)

    function new(string name, uvm_component parent=null);
        super.new(name, parent);
    endfunction

    task run_phase(uvm_phase phase);
        int d;

        phase.raise_objection(this);
        o = my_obj::type_id::create("o", this);
        repeat(80) begin
            o.randomize();
            randcase
            8: `uvm_info(get_name(), $sformatf("o = \n%s", o.sprint()), UVM_NONE)
            1: `uvm_warning(get_name(), $sformatf("o = \n%s", o.sprint()))
            endcase
            d = $urandom_range(1,5);
            #d;
        end
        phase.drop_objection(this);

    endtask

endclass

class my_env extends uvm_component;

    my_component c1, d2, d3;

  `uvm_component_utils(my_env)

    function new(string name, uvm_component parent=null);
        super.new(name, parent);
    endfunction

    function void build_phase(uvm_phase phase);
        c1 = my_component::type_id::create("c1", this);
        d2 = my_component::type_id::create("d2", this);
        d3 = my_component::type_id::create("d3", this);
    endfunction

endclass

class my_test extends uvm_test;

  `uvm_component_utils(my_test)
    my_component c1, c2, c3;
    my_env env;

    function new(string name="my_test", uvm_component parent=null);
        super.new(name, parent);
    endfunction

    function void build_phase(uvm_phase phase);
        c1 = my_component::type_id::create("c1", this);
        c2 = my_component::type_id::create("c2", this);
        c3 = my_component::type_id::create("c3", this);
        env = my_env::type_id::create("env", this);
    endfunction

endclass


module tb;

    initial begin
        run_test("my_test");
    end

endmodule

