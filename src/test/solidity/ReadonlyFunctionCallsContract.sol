// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ReadonlyFunctionCallsContract {

    struct SimpleStruct {
        bool truthy;
        string stringy;
        uint256 uinty;
    }

    struct NestedStruct {
        uint anotherUint;
        SimpleStruct simple;
        string anotherString;
    }

    struct StructWithArrays {
        uint[] uints;
        SimpleStruct[] simples;
        string[] strings;
    }

    function returningUint(uint256 input) public pure returns (uint256) {
        return input;
    }

    function returningString() public pure returns (string memory) {
        return "test";
    }

    function returningUintArray(uint256 size) public pure returns (uint256[] memory) {
        uint256[] memory res = new uint256[](size);

        for (uint i = 0; i < size; i++) {
            res[i] = i;
        }

        return res;
    }

    function returningUintArrayArray(uint256 size) public pure returns (uint256[][2] memory) {
        uint256[][2] memory res = [new uint256[](size), new uint256[](size)];

        for (uint i = 0; i < size; i++) {
            res[0][i] = i;
            res[1][i] = i;
        }

        return res;
    }

    function returningMultipleValues() public pure returns (uint256, string memory, bool[2] memory) {
        return (42, "test", [true, false]);
    }

    function returningSimpleStruct() public pure returns (SimpleStruct memory) {
        return SimpleStruct(true, "test", 42);
    }

    function returningSimpleStructArray() public pure returns (SimpleStruct[2] memory) {
        return [
            SimpleStruct(true, "test1", 42),
            SimpleStruct(false, "test2", 43)
        ];
    }

    function returningNestedStruct() public pure returns (NestedStruct memory) {
        return NestedStruct(
            1337,
            SimpleStruct(true, "test", 42),
            "elem"
        );
    }

    function returningNestedStructArray() public pure returns (NestedStruct[2] memory) {
        return [
            NestedStruct(
                1337,
                SimpleStruct(true, "test1", 42),
                "elem1"
            ),
            NestedStruct(
                1338,
                SimpleStruct(false, "test2", 43),
                "elem"
            )
        ];
    }

    function returningStructWithArrays() public pure returns (StructWithArrays memory) {
        uint256[] memory uints = new uint256[](2);
        SimpleStruct[] memory simples = new SimpleStruct[](2);
        string[] memory strings = new string[](2);

        uints[0] = 0;
        uints[1] = 1;
        simples[0] = SimpleStruct(true, "test1", 42);
        simples[1] = SimpleStruct(false, "test2", 43);
        strings[0] = "elem1";
        strings[1] = "elem2";

        return StructWithArrays(uints, simples, strings);
    }

    function returningStructWithArraysArray() public pure returns (StructWithArrays[] memory) {
        uint256[] memory uints = new uint256[](2);
        SimpleStruct[] memory simples = new SimpleStruct[](2);
        string[] memory strings = new string[](2);

        uints[0] = 0;
        uints[1] = 1;
        simples[0] = SimpleStruct(true, "test1", 42);
        simples[1] = SimpleStruct(false, "test2", 43);
        strings[0] = "elem1";
        strings[1] = "elem2";

        StructWithArrays[] memory res = new StructWithArrays[](1);

        res[0] = StructWithArrays(uints, simples, strings);

        return res;
    }
}
