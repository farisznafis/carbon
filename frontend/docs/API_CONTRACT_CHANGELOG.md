# API CONTRACT CHANGELOG

## 2026-04-22

### Added

- authentication/auth_logout.txt
	- POST /api/logout

- user_management/get_user_by_id.txt
	- GET /api/users/:userId

- profile/update_profile.txt
	- PATCH /api/profile

- profile/get_company_profile.txt
	- GET /api/company/profile

- project/get_available_projects.txt
	- GET /api/projects/available

- project/register_project.txt
	- POST /api/projects/:projectId/register

### Notes

- Added contracts align with frontend API usage and current TODO paths in src/api modules.
- register_project.txt is a generic project participation contract to complement existing solar_registration.txt.
