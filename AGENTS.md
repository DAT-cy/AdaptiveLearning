AGENTS.md — Adaptive Learning Project Instructions
This file contains persistent project-wide instructions. Read and follow it before acting on any user prompt in this repository.
The rules apply to all future tasks, prompts, phases, pages, components, and refactors unless the user explicitly overrides a rule.

1. Mandatory workflow for every task
Before making changes:
1. Read this file completely.
2. Inspect the relevant code and existing project structure. Do not assume the framework, libraries, paths, APIs, or current implementation.
3. Identify the user-facing goal, affected pages/components, dependencies, and acceptance criteria.
4. Check whether the task depends on earlier project decisions or an existing plan; preserve those decisions.
5. If a requested detail is ambiguous and materially affects implementation, ask one concise clarification. Otherwise proceed with sensible, consistent defaults.
While implementing:
1. Make changes directly in the existing codebase; do not return only a plan when implementation is requested.
2. Prefer shared components, design tokens, and reusable patterns over page-specific duplication.
3. Preserve existing business logic, API contracts, routing, authentication, authorization, data models, and integrations unless the user explicitly asks to change them.
4. Never invent backend data, fake successful API responses, or hide errors with fabricated content.
5. Avoid unrelated rewrites and unnecessary dependencies.
6. Follow the repository's existing conventions and use installed libraries where practical.
After implementing:
1. Run applicable lint, type-check, tests, and build commands that are available in the repository.
2. Inspect changed files and fix issues caused by the changes.
3. If a command cannot be run, state why. Never claim tests or builds passed unless they were actually run.
4. Report what changed, files affected, validation results, and known remaining issues.
2. Product and design direction
Product: Adaptive Learning, an AI-assisted learning and exam-preparation platform.

Design system name: Light AI Education.

The interface should feel bright, modern, calm, clear, trustworthy, accessible, and friendly to learners. It should look like a coherent professional EdTech product—not an enterprise admin dashboard, not a neon AI demo, and not a collection of unrelated templates.
Required design tokens
Use these values as the default shared palette. If the repository already has a token/theme system, integrate these values into it rather than scattering hardcoded colors.
- Primary: #2563EB
- Primary hover: #1D4ED8
- Primary light: #EFF6FF
- AI accent: #7C3AED
- AI background: #F5F3FF
- Page background: #F8FAFC
- Card/surface: #FFFFFF
- Main text: #0F172A
- Secondary text: #64748B
- Border: #E2E8F0
- Success: #16A34A
- Warning: #D97706
- Error: #DC2626
Color rules
- Blue is the main brand/action color.
- Purple is reserved primarily for AI-related features and AI status labels.
- Green, amber, and red communicate semantic status only when appropriate.
- Keep most surfaces white or very light neutral; use color intentionally and sparingly.
- Do not introduce competing teal, orange, yellow, gradients, or additional brand colors without a clear semantic reason.
- Avoid strong shadows, saturated banners, decorative gradients, and excessive colored borders.
- Maintain readable contrast for text, controls, icons, and focus states.
- Centralize colors in CSS variables, theme configuration, or the project's established token system.
3. Shared UI standards
Apply consistently across all relevant pages:
- Sidebar: white surface, subtle divider, consistent icon size, clear active item using a light blue background and blue text/indicator.
- Header: white surface, aligned spacing, restrained borders, clear search and profile controls.
- Content: light neutral page background, white cards, consistent radius and spacing, subtle borders.
- Typography: use the existing suitable font; otherwise prefer Inter or a readable sans-serif. Establish consistent heading, body, label, and metadata sizes.
- Buttons: consistent primary/secondary/tertiary styles and clear hover, focus, disabled, and loading states.
- Forms: visible labels, clear validation/error states, keyboard accessibility, and consistent spacing.
- AI elements: use purple as a restrained accent, not as a second dominant brand color.
- Empty states: explain what is missing and provide a useful next action where appropriate.
- Loading, success, error, and empty states must be designed consistently.
- Responsive behavior must work on desktop, tablet, and mobile.
- Keep the interface uncluttered but do not create excessive empty space or hide essential actions.
- Use semantic HTML and accessible interaction patterns.
4. Adaptive Learning navigation and user journeys
Keep the meaning and connections between modules clear:
- Trang chủ: learning overview, today's tasks, resume learning, and relevant progress.
- Lộ trình học: what to learn next and why; personalized sequence based on goals and available assessment data.
- Khóa học: actual learning materials, lessons, chapters, and exercises.
- Bài kiểm tra: practice tests and officially published exams; distinguish these types clearly.
- Thống kê tiến độ: learning activity, completion, assessment outcomes, and progress trends.
- Trợ lý AI: contextual explanations, help with current learning content, mistake analysis, and recommendations when supported by implemented capabilities.
- Cài đặt: account and learning preferences.
These are intended product roles, not permission to invent capabilities that the codebase/backend does not support. Inspect current behavior before changing it. Make navigation and calls to action help learners move between related modules.
5. Data integrity and UI correctness
- Never replace missing API data with fake production-looking data just to make a screen appear complete.
- Investigate suspicious titles, malformed labels, missing spaces, incorrect counts, or broken mappings at the correct layer.
- Handle null, empty, loading, and error responses explicitly.
- Format dates, durations, counts, percentages, and localized Vietnamese text consistently.
- Do not silently change API request/response contracts.
- Preserve real quota and entitlement logic. A UI must not imply a user has access to a locked feature when backend authorization says otherwise.
- Ensure buttons and links perform their advertised action; do not leave decorative controls that appear functional.
6. Prompt interpretation policy for all future requests
For every subsequent user prompt in this repository:
1. Treat this file as the persistent baseline for product design and implementation.
2. Apply the current design system automatically to new pages, features, components, and redesign tasks.
3. If the user asks for a small change, make only the necessary change while preserving consistency with this baseline.
4. If the user provides a new design requirement, reconcile it with this file. The newest explicit user instruction takes precedence where there is a direct conflict; update this file only if the user asks to make the new rule persistent.
5. Do not ask the user to repeat these design rules in every prompt.
6. Do not redesign unrelated screens unless the task calls for global consistency or shared components make it necessary.
7. If the user asks for a prompt rather than implementation, write a task-specific prompt that explicitly says to read and follow this file.
8. For multi-phase work, preserve the same tokens, components, terminology, and decisions across phases.
7. Definition of done
A task is complete only when:
- The requested behavior or UI has been implemented, not merely described, if code changes were requested.
- Existing unrelated functionality is preserved.
- Relevant pages use the shared Light AI Education system.
- Loading, empty, error, and responsive states have been considered where applicable.
- Applicable checks were run and results are reported honestly.
- Any remaining blocker or unverified behavior is clearly stated.