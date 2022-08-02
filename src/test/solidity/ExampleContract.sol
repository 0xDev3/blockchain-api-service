// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ExampleContract {

    address private _owner;

    constructor(address owner) payable {
        _owner = owner;
    }

    function setOwner(address owner) public {
        _owner = owner;
    }

    function getOwner() public view returns (address) {
        return _owner;
    }
}
