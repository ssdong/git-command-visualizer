if (process.env.NODE_ENV === "production") {
    const opt = require("./git-command-visualizer-opt.js");
    opt.main();
    module.exports = opt;
} else {
    var exports = window;
    exports.require = require("./git-command-visualizer-fastopt-entrypoint.js").require;
    window.global = window;

    const fastOpt = require("./git-command-visualizer-fastopt.js");
    fastOpt.main()
    module.exports = fastOpt;

    if (module.hot) {
        module.hot.accept();
    }
}
