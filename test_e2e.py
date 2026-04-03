#!/usr/bin/env python3
"""
End-to-end test suite for Performance Pulse API.

Tests the complete workflow:
1. Create employees
2. Create review cycle (set to ACTIVE)
3. Create goals
4. Submit reviews
5. Get employee reviews
6. Filter employees by department and rating
7. Get cycle summary with analytics

Prerequisites:
- Application running on http://localhost:8080
- Python 3.8+
- requests library (install: pip install requests)
"""

import requests
import json
from datetime import datetime, timedelta
from typing import Dict, Any, Optional
import sys

# Configuration
BASE_URL = "http://localhost:8080/api/v1"
HEADERS = {"Content-Type": "application/json"}

# ANSI color codes for output
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
BLUE = "\033[94m"
RESET = "\033[0m"
BOLD = "\033[1m"


def log_success(message: str):
    """Log success message."""
    print(f"{GREEN}✓ {message}{RESET}")


def log_error(message: str):
    """Log error message."""
    print(f"{RED}✗ {message}{RESET}")


def log_info(message: str):
    """Log info message."""
    print(f"{BLUE}→ {message}{RESET}")


def log_section(title: str):
    """Log section header."""
    print(f"\n{BOLD}{BLUE}{'='*70}{RESET}")
    print(f"{BOLD}{BLUE}{title.center(70)}{RESET}")
    print(f"{BOLD}{BLUE}{'='*70}{RESET}\n")


def print_response(response: requests.Response, title: str = "Response"):
    """Pretty print API response."""
    print(f"\n{YELLOW}[{title}]{RESET}")
    try:
        print(json.dumps(response.json(), indent=2, default=str))
    except:
        print(response.text)


class PerformancePulseE2ETest:
    """End-to-end test suite for Performance Pulse."""

    def __init__(self):
        """Initialize test suite."""
        self.session = requests.Session()
        self.session.headers.update(HEADERS)
        self.employees = {}
        self.cycles = {}
        self.reviews = []
        self.goals = []
        self.test_results = {"passed": 0, "failed": 0, "total": 0}

    def run_all_tests(self):
        """Run all tests in sequence."""
        try:
            log_section("PERFORMANCE PULSE - END-TO-END TEST SUITE")
            self.verify_api_health()
            self.test_employee_creation()
            self.test_cycle_creation()
            self.test_goal_creation()
            self.test_goal_status_updates()
            self.test_review_submission()
            self.test_review_finalization()
            self.test_employee_reviews()
            self.test_employee_filtering()
            self.test_cycle_summary()
            self.print_test_summary()
        except Exception as e:
            log_error(f"Test suite failed: {str(e)}")
            sys.exit(1)

    def verify_api_health(self):
        """Verify API is healthy."""
        log_section("1. VERIFY API HEALTH")
        try:
            response = self.session.get(f"{BASE_URL}/employees", params={"page": 0, "size": 1})
            if response.status_code in [200, 400]:
                log_success("API is healthy and responding")
                self.test_results["passed"] += 1
            else:
                log_error(f"API returned unexpected status: {response.status_code}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1
        except Exception as e:
            log_error(f"Failed to reach API: {str(e)}")
            self.test_results["failed"] += 1
            self.test_results["total"] += 1
            sys.exit(1)

    def test_employee_creation(self):
        """Test POST /employees - Create employees."""
        log_section("2. CREATE EMPLOYEES")

        employees_data = [
            {
                "firstName": "Alice",
                "lastName": "Johnson",
                "email": f"alice.johnson.{datetime.now().timestamp()}@company.com",
                "department": "ENGINEERING",
                "jobTitle": "Senior Software Engineer",
                "joiningDate": "2022-01-15",
            },
            {
                "firstName": "Bob",
                "lastName": "Smith",
                "email": f"bob.smith.{datetime.now().timestamp()}@company.com",
                "department": "SALES",
                "jobTitle": "Sales Manager",
                "joiningDate": "2023-03-20",
            },
            {
                "firstName": "Carol",
                "lastName": "Williams",
                "email": f"carol.williams.{datetime.now().timestamp()}@company.com",
                "department": "ENGINEERING",
                "jobTitle": "Junior Engineer",
                "joiningDate": "2024-01-10",
            },
            {
                "firstName": "David",
                "lastName": "Brown",
                "email": f"david.brown.{datetime.now().timestamp()}@company.com",
                "department": "HR",
                "jobTitle": "HR Manager",
                "joiningDate": "2021-06-01",
            },
        ]

        for idx, emp_data in enumerate(employees_data, 1):
            try:
                response = self.session.post(f"{BASE_URL}/employees", json=emp_data)
                if response.status_code == 201:
                    data = response.json()["data"]
                    emp_id = data["id"]
                    emp_name = f"{data['firstName']} {data['lastName']}"
                    self.employees[emp_name] = emp_id
                    log_success(f"Created employee {idx}: {emp_name} (ID: {emp_id})")
                    self.test_results["passed"] += 1
                else:
                    log_error(
                        f"Failed to create employee {idx}: {response.status_code} - {response.text}"
                    )
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception creating employee {idx}: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

        log_info(f"Employees created: {len(self.employees)}")

    def test_cycle_creation(self):
        """Test POST /cycles - Create review cycles."""
        log_section("3. CREATE REVIEW CYCLES")

        # Create 2 cycles: one ACTIVE (for submissions) and one UPCOMING
        cycles_data = [
            {
                "name": f"Q1 2026 Reviews - {datetime.now().timestamp()}",
                "startDate": (datetime.now() - timedelta(days=5)).strftime("%Y-%m-%d"),
                "endDate": (datetime.now() + timedelta(days=25)).strftime("%Y-%m-%d"),
            },
            {
                "name": f"Q2 2026 Reviews - {datetime.now().timestamp()}",
                "startDate": (datetime.now() + timedelta(days=60)).strftime("%Y-%m-%d"),
                "endDate": (datetime.now() + timedelta(days=120)).strftime("%Y-%m-%d"),
            },
        ]

        for idx, cycle_data in enumerate(cycles_data, 1):
            try:
                response = self.session.post(f"{BASE_URL}/cycles", json=cycle_data)
                if response.status_code == 201:
                    data = response.json()["data"]
                    cycle_id = data["id"]
                    cycle_name = data["name"]
                    self.cycles[cycle_name] = {"id": cycle_id, "status": "UPCOMING"}
                    log_success(f"Created cycle {idx}: {cycle_name} (ID: {cycle_id})")
                    self.test_results["passed"] += 1
                else:
                    log_error(f"Failed to create cycle {idx}: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception creating cycle {idx}: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

        # Update first cycle status to ACTIVE for reviews
        if self.cycles:
            first_cycle = list(self.cycles.values())[0]
            cycle_id = first_cycle["id"]
            try:
                response = self.session.patch(
                    f"{BASE_URL}/cycles/{cycle_id}/status",
                    json={"status": "ACTIVE"},
                )
                if response.status_code == 200:
                    first_cycle["status"] = "ACTIVE"
                    log_success(f"Updated first cycle to ACTIVE status")
                    self.test_results["passed"] += 1
                else:
                    log_error(f"Failed to update cycle status: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception updating cycle status: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

        log_info(f"Cycles created: {len(self.cycles)}")

    def test_goal_creation(self):
        """Test POST /goals - Create goals for employees."""
        log_section("4. CREATE GOALS")

        if not self.employees or not self.cycles:
            log_error("Cannot create goals: missing employees or cycles")
            return

        active_cycle = list(self.cycles.values())[0]
        cycle_id = active_cycle["id"]
        
        # Cycle date range: (now - 5 days) to (now + 25 days)
        # Due date must be within this range
        cycle_end_date = datetime.now() + timedelta(days=25)
        goal_due_date = cycle_end_date - timedelta(days=5)  # 5 days before cycle ends

        goals_count = 0
        for emp_name, emp_id in list(self.employees.items())[:3]:  # Create goals for first 3 employees
            goal_data = {
                "employeeId": emp_id,
                "cycleId": cycle_id,
                "title": f"Q1 Performance Goal for {emp_name}",
                "description": f"Deliver high-quality code and improve test coverage",
                "dueDate": goal_due_date.strftime("%Y-%m-%d"),
            }

            try:
                response = self.session.post(f"{BASE_URL}/goals", json=goal_data)
                if response.status_code == 201:
                    data = response.json()["data"]
                    goal_id = data["id"]
                    self.goals.append(
                        {"id": goal_id, "employee": emp_name, "title": data["title"]}
                    )
                    log_success(f"Created goal for {emp_name}: {data['title']}")
                    self.test_results["passed"] += 1
                    goals_count += 1
                else:
                    log_error(
                        f"Failed to create goal for {emp_name}: {response.status_code} - {response.text}"
                    )
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception creating goal for {emp_name}: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

        log_info(f"Goals created: {goals_count}")

    def test_goal_status_updates(self):
        """Test PATCH /goals/{goalId} - Update goal status."""
        log_section("5. UPDATE GOAL STATUS")

        if not self.goals:
            log_error("No goals to update")
            return

        updates_count = 0
        
        # Update first goal to IN_PROGRESS
        if len(self.goals) >= 1:
            goal = self.goals[0]
            goal_id = goal["id"]
            emp_name = goal["employee"]
            
            update_data = {
                "title": goal["title"],
                "description": "Goal in progress",
                "dueDate": (datetime.now() + timedelta(days=20)).strftime("%Y-%m-%d"),
                "status": "IN_PROGRESS"
            }
            
            try:
                response = self.session.patch(f"{BASE_URL}/goals/{goal_id}", json=update_data)
                if response.status_code == 200:
                    data = response.json()["data"]
                    log_success(f"Updated goal for {emp_name} to IN_PROGRESS status")
                    self.test_results["passed"] += 1
                    updates_count += 1
                else:
                    log_error(f"Failed to update goal to IN_PROGRESS: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception updating goal status: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1
        
        # Update second goal to COMPLETED
        if len(self.goals) >= 2:
            goal = self.goals[1]
            goal_id = goal["id"]
            emp_name = goal["employee"]
            
            update_data = {
                "title": goal["title"],
                "description": "Goal completed successfully",
                "dueDate": (datetime.now() + timedelta(days=15)).strftime("%Y-%m-%d"),
                "status": "COMPLETED"
            }
            
            try:
                response = self.session.patch(f"{BASE_URL}/goals/{goal_id}", json=update_data)
                if response.status_code == 200:
                    data = response.json()["data"]
                    log_success(f"Updated goal for {emp_name} to COMPLETED status")
                    self.test_results["passed"] += 1
                    updates_count += 1
                else:
                    log_error(f"Failed to update goal to COMPLETED: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception updating goal status: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1
        
        # Update third goal to MISSED
        if len(self.goals) >= 3:
            goal = self.goals[2]
            goal_id = goal["id"]
            emp_name = goal["employee"]
            
            update_data = {
                "title": goal["title"],
                "description": "Goal missed due to resource constraints",
                "dueDate": (datetime.now() + timedelta(days=10)).strftime("%Y-%m-%d"),
                "status": "MISSED"
            }
            
            try:
                response = self.session.patch(f"{BASE_URL}/goals/{goal_id}", json=update_data)
                if response.status_code == 200:
                    data = response.json()["data"]
                    log_success(f"Updated goal for {emp_name} to MISSED status")
                    self.test_results["passed"] += 1
                    updates_count += 1
                else:
                    log_error(f"Failed to update goal to MISSED: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception updating goal status: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1
        
        log_info(f"Goals updated: {updates_count}")

    def test_review_submission(self):
        """Test POST /reviews - Submit performance reviews."""
        log_section("6. SUBMIT PERFORMANCE REVIEWS")

        if not self.employees or not self.cycles:
            log_error("Cannot submit reviews: missing employees or cycles")
            return

        active_cycle = list(self.cycles.values())[0]
        cycle_id = active_cycle["id"]

        emp_list = list(self.employees.items())
        review_count = 0

        # Alice reviews Bob (peer review)
        if len(emp_list) >= 2:
            alice_id, emp1_name = emp_list[0][1], emp_list[0][0]
            bob_id, emp2_name = emp_list[1][1], emp_list[1][0]

            review_data = {
                "employeeId": bob_id,
                "cycleId": cycle_id,
                "reviewerId": alice_id,
                "reviewType": "PEER",
                "rating": 4,
                "notes": "Excellent collaboration and problem-solving skills",
            }

            try:
                response = self.session.post(f"{BASE_URL}/reviews", json=review_data)
                if response.status_code == 201:
                    data = response.json()["data"]
                    self.reviews.append(
                        {
                            "id": data["id"],
                            "employee": emp2_name,
                            "reviewer": emp1_name,
                            "rating": data["rating"],
                        }
                    )
                    log_success(f"Submitted review: {emp1_name} → {emp2_name} (Rating: 4)")
                    self.test_results["passed"] += 1
                    review_count += 1
                else:
                    log_error(f"Failed to submit review {emp1_name} → {emp2_name}: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception submitting review: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

        # Carol reviews Alice
        if len(emp_list) >= 3:
            carol_id, emp3_name = emp_list[2][1], emp_list[2][0]
            alice_id, emp1_name = emp_list[0][1], emp_list[0][0]

            review_data = {
                "employeeId": alice_id,
                "cycleId": cycle_id,
                "reviewerId": carol_id,
                "reviewType": "PEER",
                "rating": 5,
                "notes": "Outstanding technical lead and mentor",
            }

            try:
                response = self.session.post(f"{BASE_URL}/reviews", json=review_data)
                if response.status_code == 201:
                    data = response.json()["data"]
                    self.reviews.append(
                        {
                            "id": data["id"],
                            "employee": emp1_name,
                            "reviewer": emp3_name,
                            "rating": data["rating"],
                        }
                    )
                    log_success(f"Submitted review: {emp3_name} → {emp1_name} (Rating: 5)")
                    self.test_results["passed"] += 1
                    review_count += 1
                else:
                    log_error(f"Failed to submit review {emp3_name} → {emp1_name}: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception submitting review: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

        # David's self-review
        if len(emp_list) >= 4:
            david_id, emp4_name = emp_list[3][1], emp_list[3][0]

            review_data = {
                "employeeId": david_id,
                "cycleId": cycle_id,
                "reviewType": "SELF",
                "rating": 3,
                "notes": "Good performance, seeking improvement areas",
            }

            try:
                response = self.session.post(f"{BASE_URL}/reviews", json=review_data)
                if response.status_code == 201:
                    data = response.json()["data"]
                    self.reviews.append(
                        {
                            "id": data["id"],
                            "employee": emp4_name,
                            "reviewer": "Self",
                            "rating": data["rating"],
                        }
                    )
                    log_success(f"Submitted self-review: {emp4_name} (Rating: 3)")
                    self.test_results["passed"] += 1
                    review_count += 1
                else:
                    log_error(f"Failed to submit self-review for {emp4_name}: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception submitting self-review: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

        log_info(f"Reviews submitted: {review_count}")

    def test_review_finalization(self):
        """Test PATCH /reviews/{id}/finalize - Finalize submitted reviews."""
        log_section("7. FINALIZE PERFORMANCE REVIEWS")

        if not self.reviews:
            log_error("No reviews to finalize")
            return

        finalized_count = 0
        for review in self.reviews:
            review_id = review["id"]
            emp_name = review["employee"]
            reviewer_name = review["reviewer"]
            
            try:
                response = self.session.patch(f"{BASE_URL}/reviews/{review_id}/finalize")
                if response.status_code == 200:
                    data = response.json()["data"]
                    log_success(f"Finalized review: {reviewer_name} → {emp_name} (Rating: {data['rating']})")
                    self.test_results["passed"] += 1
                    finalized_count += 1
                else:
                    log_error(f"Failed to finalize review {review_id}: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception finalizing review: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

        log_info(f"Reviews finalized: {finalized_count}")

    def test_employee_reviews(self):
        """Test GET /employees/{id}/reviews - Get all reviews for an employee."""
        log_section("8. GET EMPLOYEE REVIEWS")

        emp_list = list(self.employees.items())
        if not emp_list:
            log_error("No employees to fetch reviews for")
            return

        # Get reviews for first 2 employees
        for emp_name, emp_id in emp_list[:2]:
            try:
                response = self.session.get(
                    f"{BASE_URL}/employees/{emp_id}/reviews",
                    params={"page": 0, "size": 20},
                )
                if response.status_code == 200:
                    data = response.json()["data"]
                    review_count = len(data.get("content", []))
                    log_success(f"Fetched {review_count} reviews for {emp_name}")
                    if review_count > 0:
                        log_info(f"  - Sample review: {data['content'][0]['cycleName']}")
                    self.test_results["passed"] += 1
                else:
                    log_error(f"Failed to fetch reviews for {emp_name}: {response.status_code}")
                    self.test_results["failed"] += 1
            except Exception as e:
                log_error(f"Exception fetching reviews for {emp_name}: {str(e)}")
                self.test_results["failed"] += 1
            self.test_results["total"] += 1

    def test_employee_filtering(self):
        """Test GET /employees?department={dept}&minRating={x} - Filter employees."""
        log_section("9. FILTER EMPLOYEES BY DEPARTMENT & RATING")

        # Test 1: Filter by department
        try:
            response = self.session.get(
                f"{BASE_URL}/employees",
                params={"department": "ENGINEERING", "page": 0, "size": 20},
            )
            if response.status_code == 200:
                data = response.json()["data"]
                count = len(data.get("content", []))
                log_success(f"Found {count} ENGINEERING department employees")
                self.test_results["passed"] += 1
            else:
                log_error(f"Failed to filter by department: {response.status_code}")
                self.test_results["failed"] += 1
        except Exception as e:
            log_error(f"Exception filtering by department: {str(e)}")
            self.test_results["failed"] += 1
        self.test_results["total"] += 1

        # Test 2: Filter by minRating (requires reviews data)
        try:
            response = self.session.get(
                f"{BASE_URL}/employees",
                params={"minRating": 3.0, "page": 0, "size": 20},
            )
            if response.status_code == 200:
                data = response.json()["data"]
                count = len(data.get("content", []))
                log_success(f"Found {count} employees with rating >= 3.0")
                self.test_results["passed"] += 1
            else:
                log_error(f"Failed to filter by minRating: {response.status_code}")
                self.test_results["failed"] += 1
        except Exception as e:
            log_error(f"Exception filtering by minRating: {str(e)}")
            self.test_results["failed"] += 1
        self.test_results["total"] += 1

        # Test 3: Filter by both department AND minRating
        try:
            response = self.session.get(
                f"{BASE_URL}/employees",
                params={"department": "ENGINEERING", "minRating": 2.0, "page": 0, "size": 20},
            )
            if response.status_code == 200:
                data = response.json()["data"]
                count = len(data.get("content", []))
                log_success(
                    f"Found {count} ENGINEERING employees with rating >= 2.0 (cached)"
                )
                self.test_results["passed"] += 1
            else:
                log_error(
                    f"Failed to filter by department + minRating: {response.status_code}"
                )
                self.test_results["failed"] += 1
        except Exception as e:
            log_error(f"Exception filtering by department + minRating: {str(e)}")
            self.test_results["failed"] += 1
        self.test_results["total"] += 1

    def test_cycle_summary(self):
        """Test GET /cycles/{id}/summary - Get cycle analytics summary."""
        log_section("10. GET CYCLE SUMMARY & ANALYTICS")

        if not self.cycles:
            log_error("No cycles to analyze")
            return

        active_cycle = list(self.cycles.values())[0]
        cycle_id = active_cycle["id"]

        try:
            response = self.session.get(f"{BASE_URL}/cycles/{cycle_id}/summary")
            if response.status_code == 200:
                data = response.json()["data"]
                log_success(f"Retrieved cycle summary (cached)")
                log_info(f"  - Cycle: {data['cycleName']}")
                log_info(
                    f"  - Total Reviews: {data['totalReviews']}"
                )
                log_info(
                    f"  - Average Rating: {data['averageRating']}"
                )
                if data.get("topPerformer"):
                    log_info(
                        f"  - Top Performer: {data['topPerformer']['name']} "
                        f"(Rating: {data['topPerformer']['averageRating']})"
                    )
                if data.get("goalStats"):
                    stats = data["goalStats"]
                    log_info(
                        f"  - Goal Stats: Total={stats['total']}, "
                        f"Completed={stats['completed']}, "
                        f"Missed={stats['missed']}, "
                        f"In Progress={stats['inProgress']}"
                    )
                    if stats['total'] > 0:
                        log_info(
                            f"  - Goal Completion Rate: {stats.get('completionRate', 0):.1f}%"
                        )
                self.test_results["passed"] += 1
            else:
                log_error(f"Failed to get cycle summary: {response.status_code}")
                if response.text:
                    log_error(f"  Response: {response.text}")
                self.test_results["failed"] += 1
        except Exception as e:
            log_error(f"Exception getting cycle summary: {str(e)}")
            self.test_results["failed"] += 1
        self.test_results["total"] += 1

    def print_test_summary(self):
        """Print test execution summary."""
        log_section("TEST EXECUTION SUMMARY")

        passed = self.test_results["passed"]
        failed = self.test_results["failed"]
        total = self.test_results["total"]

        print(f"Total Tests:  {BOLD}{total}{RESET}")
        print(f"Passed:       {GREEN}{BOLD}{passed}{RESET}")
        print(f"Failed:       {RED}{BOLD}{failed}{RESET}")
        print(f"Success Rate: {BOLD}{(passed/total*100):.1f}%{RESET}\n")

        if failed == 0:
            log_success("All tests passed! ✓")
        else:
            log_error(f"{failed} test(s) failed")

        print(f"\n{BLUE}{'='*70}{RESET}\n")


def main():
    """Main entry point."""
    print(f"\n{BOLD}Performance Pulse - End-to-End Test Suite{RESET}")
    print(f"Starting at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    tester = PerformancePulseE2ETest()
    tester.run_all_tests()

    # Exit with appropriate code
    if tester.test_results["failed"] > 0:
        sys.exit(1)
    else:
        sys.exit(0)


if __name__ == "__main__":
    main()
