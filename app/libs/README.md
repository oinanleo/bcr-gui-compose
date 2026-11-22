这个目录放不通过 Maven 分发的本地依赖，**不进版本库**（见根目录 `.gitignore`）。

构建前跑一次：

```bash
bash scripts/fetch-deps.sh
```

会下载 `sherpa-onnx-2.0.2.aar`（约 50 MB，来自
[k2-fsa/sherpa-onnx v2.0.2 release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.2)），
`app/build.gradle.kts` 里以 `implementation(files("libs/sherpa-onnx-2.0.2.aar"))` 引用。
