import styles from "../styles/Home.module.scss"

export default function Home() {
  return (
    <div className={styles.container}>
      <main className={styles.main}>
        <h1 className={styles.title}>
          Welcome to <a href="https://bingo.jtprince.com/">MultiBingo</a>!
        </h1>

        <p className={styles.description}>
          This page is currently in development.
        </p>
      </main>
    </div>
  )
}
