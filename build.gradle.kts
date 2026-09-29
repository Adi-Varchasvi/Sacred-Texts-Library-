// Root build file. Shared build logic lives in the :extension module.
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
