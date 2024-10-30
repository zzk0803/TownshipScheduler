package zzk.townshipscheduler.backend.tfdemo.projectscheduling;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static java.util.Collections.emptyList;
import static zzk.townshipscheduler.backend.tfdemo.projectscheduling.JobType.*;

public class DemoDataGenerator {

    private static final Project FIRST_PROJECT = new Project("0", 0, 10);

    private static final Project SECOND_PROJECT = new Project("1", 4, 19);

    private final Random RANDOM = new Random(0);

    public ProjectJobSchedule generateDemoData() {
        ProjectJobSchedule projectJobSchedule = new ProjectJobSchedule();

        // Projects
        List<Project> projects = List.of(FIRST_PROJECT, SECOND_PROJECT);
        // Resources
        List<Resource> resources = List.of(
                new GlobalResource("0", 16),
                new LocalResource("1", FIRST_PROJECT, 13, true),
                new LocalResource("2", FIRST_PROJECT, 44, false),
                new LocalResource("3", FIRST_PROJECT, 39, false),
                new LocalResource("4", SECOND_PROJECT, 24, true),
                new LocalResource("5", SECOND_PROJECT, 66, false),
                new LocalResource("6", SECOND_PROJECT, 56, false)
        );
        // Jobs
        List<Job> jobs = generateJobs(24, projects, resources);
        // Allocations
        List<Allocation> allocations = generateAllocations(jobs);
        // Update schedule
        projectJobSchedule.setProjects(projects);
        projectJobSchedule.setResources(resources);
        projectJobSchedule.setJobs(jobs);
        projectJobSchedule.setAllocations(allocations);
        projectJobSchedule.setExecutionModes(jobs.stream().flatMap(job -> job.getExecutionModes().stream()).toList());
        projectJobSchedule.setResourceRequirements(
                projectJobSchedule.getExecutionModes()
                        .stream()
                        .flatMap(e -> e.getResourceRequirements().stream())
                        .toList());

        return projectJobSchedule;
    }

    private List<Job> generateJobs(int jobsSize, List<Project> projects, List<Resource> resources) {
        List<Job> jobs = new ArrayList<>(jobsSize);

        int jobsCountPerProject = jobsSize / 2;
        int countJob = 0;
        for (Project project : projects) {
            // Generate the job list
            List<Job> jobsPerProject = new ArrayList<>(jobsCountPerProject);
            jobsPerProject.add(new Job(String.valueOf(countJob++), project, SOURCE));
            for (int i = 0; i < jobsCountPerProject - 2; i++) {
                jobsPerProject.add(new Job(String.valueOf(countJob++), project, STANDARD));
            }
            jobsPerProject.add(new Job(String.valueOf(countJob++), project, SINK));

            // Add all jobs of the given project
            jobs.addAll(jobsPerProject);

            // Add the execution modes
            jobsPerProject.forEach(job -> generateExecutionModes(
                    jobs,
                    job,
                    resources.stream()
                            .filter(r -> r.getId().equals("0") || ((LocalResource) r).getProject().equals(project))
                            .toList()
            ));

            // Add the successor jobs
            List<Pair<Float, Integer>> successorJobsProb = List.of(
                    new Pair<>(0.54f, 1), // 54% - one job
                    new Pair<>(0.81f, 2), // 27% - two jobs, etc
                    new Pair<>(1f, 3)
            );
            Job firstJob = jobs.stream()
                    .filter(j -> j.getProject().equals(project) && j.getJobType() == SOURCE)
                    .findFirst()
                    .get();
            firstJob.setSuccessorJobs(jobs.stream()
                    .filter(j -> j.getProject().equals(project) && j.getJobType() == STANDARD)
                    .toList()
                    .subList(0, 3));
            for (int i = 1; i < jobsCountPerProject; i++) {
                double jProb = RANDOM.nextDouble();
                int countSuccessorJobs = successorJobsProb.stream()
                        .filter(rs -> jProb <= rs.key())
                        .map(Pair::value)
                        .findFirst()
                        .get();
                if (countSuccessorJobs > jobsCountPerProject - i - 1) {
                    countSuccessorJobs = jobsCountPerProject - i - 1;
                }
                List<Job> successorJobs = new ArrayList<>(countSuccessorJobs);
                while (successorJobs.size() < countSuccessorJobs) {
                    int jobIdx = RANDOM.nextInt(i + 1, jobsCountPerProject);
                    if (!successorJobs.contains(jobsPerProject.get(jobIdx))) {
                        successorJobs.add(jobsPerProject.get(jobIdx));
                    }
                }
                successorJobs.sort(Comparator.comparing(Job::getId));
                jobsPerProject.get(i).setSuccessorJobs(successorJobs);
            }
            jobsPerProject.get(jobsCountPerProject - 2)
                    .setSuccessorJobs(List.of(jobsPerProject.get(jobsCountPerProject - 1)));
            jobsPerProject.get(jobsCountPerProject - 1).setSuccessorJobs(emptyList());
        }
        return jobs;
    }

    private void generateExecutionModes(List<Job> jobs, Job job, List<Resource> resources) {
        int countExecutionMode = (int) jobs.stream()
                .filter(j -> j.getExecutionModes() != null)
                .mapToLong(j -> j.getExecutionModes().size())
                .sum();
        int countRequirements = (int) jobs.stream()
                .filter(j -> j.getExecutionModes() != null)
                .flatMap(e -> e.getExecutionModes().stream())
                .filter(e -> e.getResourceRequirements() != null)
                .mapToLong(e -> e.getResourceRequirements().size())
                .sum();

        if (job.getJobType() == SOURCE || job.getJobType() == SINK) {
            job.setExecutionModes(
                    List.of(
                            new ExecutionMode(String.valueOf(countExecutionMode), job, 0, emptyList())
                    )
            );
        } else if (job.getJobType() == STANDARD) {
            List<Pair<Float, Integer>> resourcesProb = List.of(
                    new Pair<>(0.4f, 0), // 40% - global resource
                    new Pair<>(0.6f, 1), // 20% - resource 1, etc
                    new Pair<>(0.75f, 2),
                    new Pair<>(1f, 3)
            );
            List<ExecutionMode> executionModes = new ArrayList<>(3);

            // Three execution modes
            int requirementsSize = RANDOM.nextInt(1, 4);
            for (int i = 0; i < 3; i++) {
                // [1, 5] duration
                ExecutionMode executionMode = new ExecutionMode(
                        String.valueOf(countExecutionMode++),
                        job,
                        RANDOM.nextInt(1, 6)
                );
                List<ResourceRequirement> requirements = new ArrayList<>(requirementsSize);

                while (requirements.size() < requirementsSize) {
                    double rProb = RANDOM.nextDouble();
                    Resource resource = resourcesProb.stream()
                            .filter(rs -> rProb <= rs.key())
                            .map(Pair::value)
                            .findFirst()
                            .map(resources::get)
                            .get();
                    if (requirements.stream().noneMatch(r -> r.getResource().equals(resource))) {
                        // [1, 5] requirement
                        requirements.add(new ResourceRequirement(String.valueOf(countRequirements++), executionMode,
                                resource, RANDOM.nextInt(1, 6)
                        ));
                    }
                }
                executionMode.setResourceRequirements(requirements);
                executionModes.add(executionMode);
            }
            job.setExecutionModes(executionModes);
        }
    }

    private List<Allocation> generateAllocations(List<Job> jobs) {
        List<Allocation> allocations = new ArrayList<>(jobs.size());
        int doneDate = 0;
        for (int i = 0; i < jobs.size(); i++) {
            allocations.add(new Allocation(String.valueOf(i), jobs.get(i)));
        }
        // Set source, sink, predecessor and successor jobs
        for (int i = 0; i < jobs.size(); i++) {
            Allocation allocation = allocations.get(i);
            Allocation sourceAllocation = allocations.stream()
                    .filter(a -> a.getJob().getJobType() == SOURCE
                                 && a.getJob().getProject().equals(allocation.getJob().getProject()))
                    .findFirst()
                    .get();
            Allocation sinkAllocation = allocations.stream()
                    .filter(a -> a.getJob().getJobType() == SINK
                                 && a.getJob().getProject().equals(allocation.getJob().getProject()))
                    .findFirst()
                    .get();
            List<Allocation> predecessorAllocations = allocations.stream()
                    .filter(a -> !a.equals(allocation) && a.getJob().getSuccessorJobs().contains(allocation.getJob()))
                    .distinct()
                    .toList();
            List<Allocation> successorAllocations = allocation.getJob().getSuccessorJobs().stream()
                    .map(j -> allocations.stream().filter(a -> a.getJob().equals(j)).findFirst().get())
                    .toList();
            allocation.setSourceAllocation(sourceAllocation);
            allocation.setSinkAllocation(sinkAllocation);
            allocation.setPredecessorAllocations(predecessorAllocations);
            allocation.setSuccessorAllocations(successorAllocations);
            allocation.setPredecessorsDoneDate(doneDate);
            boolean isSource = allocation.getJob().getJobType() == SOURCE;
            boolean isSink = allocation.getJob().getJobType() == SINK;
            if (isSource || isSink) {
                allocation.setExecutionMode(allocation.getJob().getExecutionModes().get(0));
                allocation.setDelay(0);
                if (isSink) {
                    doneDate += 4;
                }
            }
        }

        return allocations;
    }

    private record Pair<K, V>(K key, V value) {

    }

}
