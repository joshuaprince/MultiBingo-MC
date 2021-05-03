import {
  Button,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay
} from "@chakra-ui/react"
import React from "react"

import styles from "styles/SpaceTouchModal.module.scss"

import { ISpace } from "../../interface/ISpace"
import { ColorPicker } from "./ColorPicker"

type IProps = {
  isOpen: boolean
  close: () => void
  space: ISpace
}

export const SpaceTouchModal: React.FunctionComponent<IProps> = (props) => {
  return (
    <Modal size="xs" isOpen={props.isOpen} onClose={props.close}>
      <ModalOverlay/>
      <ModalContent>
        <ModalCloseButton/>
        <ModalHeader className={styles.header}>{props.space.text}</ModalHeader>

        <ModalBody className={styles.body}>
          {props.space.tooltip &&
            <p className={styles.tooltipText}>{props.space.tooltip}</p>
          }

          {props.space.auto &&
            <p className={styles.autoText}>
              This space will be marked automatically when you complete the objective in-game.
            </p>
          }

          <ColorPicker
            spaceId={props.space.space_id}
            onClick={props.close}
            className={styles.colorPickerModal}
          />
        </ModalBody>

        <ModalFooter>
          <Button colorScheme="blue" onClick={props.close}>Close</Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  )
}
