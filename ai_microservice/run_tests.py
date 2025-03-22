"""
Script to run the test suite with different options
"""
import os
import sys
import subprocess


def run_unit_tests():
    """Run only unit tests"""
    print("Running unit tests...")
    result = subprocess.run(["pytest", "-v", "--ignore=tests/integration"])
    return result.returncode


def run_integration_tests():
    """Run only integration tests"""
    print("Running integration tests...")
    result = subprocess.run(["pytest", "-v", "-m", "integration", "tests/integration"])
    return result.returncode


def run_all_tests():
    """Run all tests including integration tests"""
    print("Running all tests...")
    result = subprocess.run(["pytest", "-v"])
    return result.returncode


def run_specific_test(test_path):
    """Run a specific test file or directory"""
    print(f"Running specific test: {test_path}")
    result = subprocess.run(["pytest", "-v", test_path])
    return result.returncode


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python run_tests.py [unit|integration|all|path/to/test]")
        sys.exit(1)
    
    command = sys.argv[1].lower()
    
    if command == "unit":
        sys.exit(run_unit_tests())
    elif command == "integration":
        sys.exit(run_integration_tests())
    elif command == "all":
        sys.exit(run_all_tests())
    else:
        # Assume it's a path to a specific test file or directory
        sys.exit(run_specific_test(sys.argv[1]))